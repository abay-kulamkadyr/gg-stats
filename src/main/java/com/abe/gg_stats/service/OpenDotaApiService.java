package com.abe.gg_stats.service;

import com.abe.gg_stats.entity.ApiRateLimit;
import com.abe.gg_stats.repository.ApiRateLimitRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Service
@RequiredArgsConstructor
@Slf4j
public class OpenDotaApiService {

	private final RestTemplate restTemplate;

	private final ApiRateLimitRepository rateLimitRepository;

	private final ObjectMapper objectMapper;

	@Value("${opendota.api.base-url:https://api.opendota.com/api}")
	private String baseUrl;

	@Value("${opendota.api.rate-limit.per-minute:50}")
	private int requestsPerMinute;

	@Value("${opendota.api.rate-limit.per-day:1800}")
	private int requestsPerDay;

	@Value("${opendota.api.rate-limit.window-size-minutes:1}")
	private int windowSizeMinutes;

	@Value("${opendota.api.timeout.connection:5000}")
	private int connectionTimeout;

	@Value("${opendota.api.timeout.read:10000}")
	private int readTimeout;

	@Value("${opendota.api.circuit-breaker.threshold:5}")
	private int circuitBreakerThreshold;

	@Value("${opendota.api.circuit-breaker.timeout:30000}")
	private long circuitBreakerTimeout;

	// In-memory locks per endpoint to prevent race conditions
	private final Map<String, ReentrantLock> endpointLocks = new ConcurrentHashMap<>();

	// Circuit breaker state
	private final AtomicInteger failureCount = new AtomicInteger(0);

	private final AtomicLong lastFailureTime = new AtomicLong(0);

	private volatile boolean circuitOpen = false;

	/**
	 * Makes a rate-limited API call to OpenDota with circuit breaker protection
	 */
	public Optional<JsonNode> makeApiCall(String endpoint) {
		if (!canMakeRequest(endpoint)) {
			log.warn("Rate limit exceeded for endpoint: {}", endpoint);
			return Optional.empty();
		}

		if (isCircuitOpen()) {
			log.warn("Circuit breaker is open for endpoint: {}, skipping request", endpoint);
			return Optional.empty();
		}

		try {
			String url = baseUrl + endpoint;
			log.debug("Making API call to: {}", url);

			ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

			if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
				updateRateLimit(endpoint);
				resetCircuitBreaker();
				return Optional.of(objectMapper.readTree(response.getBody()));
			}

		}
		catch (HttpClientErrorException e) {
			if (e.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
				log.warn("Rate limited by OpenDota API for endpoint: {}", endpoint);
				forceUpdateRateLimit(endpoint);
			}
			else {
				log.error("HTTP error calling OpenDota API: {} - {}", e.getStatusCode(), e.getMessage());
				recordFailure();
			}
		}
		catch (ResourceAccessException e) {
			log.error("Connection error calling OpenDota API: {}", e.getMessage());
			recordFailure();
		}
		catch (Exception e) {
			log.error("Error calling OpenDota API: {}", e.getMessage(), e);
			recordFailure();
		}

		return Optional.empty();
	}

	/**
	 * Circuit breaker implementation
	 */
	private boolean isCircuitOpen() {
		if (circuitOpen) {
			long timeSinceLastFailure = System.currentTimeMillis() - lastFailureTime.get();
			if (timeSinceLastFailure > circuitBreakerTimeout) {
				log.info("Circuit breaker timeout reached, attempting to close circuit");
				circuitOpen = false;
				failureCount.set(0);
				return false;
			}
			return true;
		}
		return false;
	}

	private void recordFailure() {
		int failures = failureCount.incrementAndGet();
		lastFailureTime.set(System.currentTimeMillis());

		if (failures >= circuitBreakerThreshold) {
			circuitOpen = true;
			log.warn("Circuit breaker opened after {} failures", failures);
		}
	}

	private void resetCircuitBreaker() {
		failureCount.set(0);
		if (circuitOpen) {
			circuitOpen = false;
			log.info("Circuit breaker closed after successful request");
		}
	}

	/**
	 * Gets circuit breaker status
	 */
	public String getCircuitBreakerStatus() {
		return String.format("Circuit Breaker Status - Open: %s, Failures: %d, Last Failure: %dms ago", circuitOpen,
				failureCount.get(), System.currentTimeMillis() - lastFailureTime.get());
	}

	/**
	 * Checks if we can make a request based on rate limits Uses proper locking to prevent
	 * race conditions
	 */
	private boolean canMakeRequest(String endpoint) {
		ReentrantLock lock = endpointLocks.computeIfAbsent(endpoint, k -> new ReentrantLock());

		try {
			lock.lock();

			LocalDateTime now = LocalDateTime.now();
			LocalDate today = LocalDate.now();

			// Check daily limit first
			Integer totalDailyRequests = rateLimitRepository.getTotalDailyRequests(today);
			if (totalDailyRequests != null && totalDailyRequests >= requestsPerDay) {
				log.warn("Daily rate limit of {} requests exceeded", requestsPerDay);
				return false;
			}

			// Get or create rate limit record for this endpoint
			ApiRateLimit rateLimit = rateLimitRepository.findByEndpoint(endpoint)
				.orElseGet(() -> createNewRateLimit(endpoint, now, today));

			// Check if we need to reset the minute window
			if (ChronoUnit.MINUTES.between(rateLimit.getWindowStart(), now) >= windowSizeMinutes) {
				rateLimitRepository.resetMinuteWindow(rateLimit.getId(), now);
				rateLimit.setRequestsCount(1);
				rateLimit.setWindowStart(now);
			}

			// Check if we need to reset the daily window
			if (!rateLimit.getDailyWindowStart().equals(today)) {
				rateLimitRepository.resetDailyWindow(rateLimit.getId(), today);
				rateLimit.setDailyRequests(1);
				rateLimit.setDailyWindowStart(today);
			}

			// Check if we can make the request
			boolean canMake = rateLimit.getRequestsCount() < requestsPerMinute;

			if (!canMake) {
				log.debug("Rate limit exceeded for {}: {}/{} requests in current window", endpoint,
						rateLimit.getRequestsCount(), requestsPerMinute);
			}

			return canMake;
		}
		finally {
			lock.unlock();
		}
	}

	/**
	 * Creates a new rate limit record for an endpoint
	 */
	private ApiRateLimit createNewRateLimit(String endpoint, LocalDateTime now, LocalDate today) {
		ApiRateLimit rateLimit = ApiRateLimit.builder()
			.endpoint(endpoint)
			.requestsCount(1)
			.windowStart(now)
			.dailyRequests(1)
			.dailyWindowStart(today)
			.updatedAt(now)
			.build();

		return rateLimitRepository.save(rateLimit);
	}

	/**
	 * Updates rate limit after successful API call
	 */
	@Transactional
	private void updateRateLimit(String endpoint) {
		Optional<ApiRateLimit> rateLimitOpt = rateLimitRepository.findByEndpoint(endpoint);
		rateLimitOpt.ifPresent(rateLimit -> rateLimitRepository.incrementRequestCounts(rateLimit.getId()));
	}

	/**
	 * Forces rate limit update when API returns 429
	 */
	@Transactional
	private void forceUpdateRateLimit(String endpoint) {
		Optional<ApiRateLimit> rateLimitOpt = rateLimitRepository.findByEndpoint(endpoint);
		if (rateLimitOpt.isPresent()) {
			ApiRateLimit rateLimit = rateLimitOpt.get();
			// Force the rate limit to be at maximum to prevent further calls
			rateLimit.setRequestsCount(requestsPerMinute);
			rateLimitRepository.save(rateLimit);
		}
	}

	/**
	 * Gets remaining requests for today
	 */
	public int getRemainingDailyRequests() {
		Integer used = rateLimitRepository.getTotalDailyRequests(LocalDate.now());
		return requestsPerDay - (used != null ? used : 0);
	}

	/**
	 * Gets remaining requests for current minute window for a specific endpoint
	 */
	public int getRemainingMinuteRequests(String endpoint) {
		Optional<ApiRateLimit> rateLimit = rateLimitRepository.findByEndpoint(endpoint);
		if (rateLimit.isPresent()) {
			ApiRateLimit rl = rateLimit.get();
			LocalDateTime now = LocalDateTime.now();

			// If window has expired, reset count
			if (ChronoUnit.MINUTES.between(rl.getWindowStart(), now) >= windowSizeMinutes) {
				return requestsPerMinute;
			}

			return Math.max(0, requestsPerMinute - rl.getRequestsCount());
		}
		return requestsPerMinute;
	}

	/**
	 * Gets current rate limit status for an endpoint
	 */
	public Optional<ApiRateLimit> getRateLimitStatus(String endpoint) {
		return rateLimitRepository.findByEndpoint(endpoint);
	}

	/**
	 * Specific API methods
	 */
	public Optional<JsonNode> getHeroes() {
		return makeApiCall("/heroes");
	}

	public Optional<JsonNode> getProPlayers() {
		return makeApiCall("/proPlayers");
	}

	public Optional<JsonNode> getTeams() {
		return makeApiCall("/teams");
	}

	public Optional<JsonNode> getPlayer(Long accountId) {
		return makeApiCall("/players/" + accountId);
	}

	public Optional<JsonNode> getPlayerRanking(Long accountId) {
		return makeApiCall("/players/" + accountId + "/rankings");
	}

	public Optional<JsonNode> getHeroRanking(Integer heroId) {
		return makeApiCall("/rankings?hero_id=" + heroId);
	}

}