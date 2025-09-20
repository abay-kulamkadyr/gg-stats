package com.abe.gg_stats.service;

import com.abe.gg_stats.exception.ApiServiceException;
import com.abe.gg_stats.exception.CircuitBreakerOpenException;
import com.abe.gg_stats.service.circuit_breaker.CircuitBreakerService;
import com.abe.gg_stats.service.rate_limit.OpenDotaRateLimitingService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

@Service
@Slf4j
public class OpenDotaApiService implements HealthIndicator {

	private static final String SERVICE_NAME = "opendota_api";

	private final RestTemplate restTemplate;

	private final OpenDotaRateLimitingService openDotaRateLimitingService;

	private final CircuitBreakerService circuitBreakerService;

	private final ObjectMapper objectMapper;

	private final MeterRegistry meterRegistry;

	@Value("${opendota.api.base-url:https://api.opendota.com/api}")
	private String baseUrl;

	@Value("${opendota.api.timeout.read:30000}")
	private long readTimeoutMs;

	@Value("${opendota.api.timeout.connect:10000}")
	private long connectTimeoutMs;

	@Value("${opendota.api.health-check.enabled:true}")
	private boolean healthCheckEnabled;

	@Autowired
	public OpenDotaApiService(RestTemplate restTemplate, OpenDotaRateLimitingService rateLimitingService,
			CircuitBreakerService circuitBreakerService, ObjectMapper objectMapper, MeterRegistry meterRegistry) {
		this.restTemplate = restTemplate;
		this.openDotaRateLimitingService = rateLimitingService;
		this.circuitBreakerService = circuitBreakerService;
		this.objectMapper = objectMapper;
		this.meterRegistry = meterRegistry;
	}

	@PostConstruct
	public void initialize() {
		log.info("Initialized OpenDotaApiService, baseUrl={}, readTimeoutMs={}, connectionTimeoutMs={}", baseUrl,
				readTimeoutMs, connectTimeoutMs);
	}

	public Optional<JsonNode> getHeroes() {
		return performApiCallWithCircuitBreaker("/heroes");
	}

	public Optional<JsonNode> getProPlayers() {
		return performApiCallWithCircuitBreaker("/proPlayers");
	}

	public Optional<JsonNode> getTeams() {
		return performApiCallWithCircuitBreaker("/teams");
	}

	public Optional<JsonNode> getTeamsPage(int page) {
		if (page < 1) {
			page = 1;
		}
		if (page > 50) {
			page = 50;
		}
		return performApiCallWithCircuitBreaker("/teams?page=" + page);
	}

	public Optional<JsonNode> getProMatchesPage(Long lessThanMatchId) {
		String endpoint = "/proMatches" + (lessThanMatchId != null ? "?less_than_match_id=" + lessThanMatchId : "");
		return performApiCallWithCircuitBreaker(endpoint);
	}

	public Optional<JsonNode> getMatchDetail(long matchId) {
		return performApiCallWithCircuitBreaker("/matches/" + matchId);
	}

	public Optional<JsonNode> getPatches() {
		return performApiCallWithCircuitBreaker("/constants/patch");
	}

	public Optional<JsonNode> getPlayer(Long accountId) {
		if (accountId == null || accountId <= 0) {
			log.warn("Invalid account ID provided: {}", accountId);
			return Optional.empty();
		}
		return performApiCallWithCircuitBreaker("/players/" + accountId);
	}

	public Optional<JsonNode> getHeroRanking(Integer heroId) {
		if (heroId == null || heroId <= 0) {
			log.warn("Invalid hero ID provided: {}", heroId);
			return Optional.empty();
		}
		return performApiCallWithCircuitBreaker("/rankings?hero_id=" + heroId);
	}

	private Optional<JsonNode> performApiCallWithCircuitBreaker(String endpoint) {
		try {
			return circuitBreakerService.executeWithCircuitBreaker(SERVICE_NAME, () -> performApiCall(endpoint),
					() -> handleFallback(endpoint));
		}
		catch (CircuitBreakerOpenException e) {
			log.warn("Circuit breaker prevented API call, endpoint={}, reason={}", endpoint, e.getMessage());
			throw new ApiServiceException("OpenDotaApiService", endpoint, "Circuit breaker prevented API call", e);
		}
	}

	private Optional<JsonNode> performApiCall(String endpoint) {
		Timer.Sample sample = Timer.start(meterRegistry);
		String statusTag = "unknown";
		long responseSize = -1;
		long durationMs;

		try {
			// Rate limiting
			OpenDotaRateLimitingService.RateLimitResult rateLimitResult = openDotaRateLimitingService
				.tryAcquirePermit(endpoint);

			if (!rateLimitResult.allowed()) {
				statusTag = "rate_limited";
				log.warn("Rate limit exceeded, endpoint={}, reason={}, resetTimeMs={}", endpoint,
						rateLimitResult.reason(), rateLimitResult.resetTimeMs());
				return Optional.empty();
			}

			String url = baseUrl + endpoint;
			Instant startTime = Instant.now();
			ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
			durationMs = Duration.between(startTime, Instant.now()).toMillis();

			if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
				statusTag = String.valueOf(response.getStatusCode().value());
				responseSize = response.getBody().length();
				log.trace("API call success, endpoint={}, status={}, durationMs={}, size={}", endpoint,
						response.getStatusCode().value(), durationMs, responseSize);
				return Optional.of(objectMapper.readTree(response.getBody()));
			}
			else if (response.getStatusCode() == HttpStatus.OK) {
				statusTag = String.valueOf(response.getStatusCode().value());
				log.warn("API call returned null body, endpoint={}", endpoint);
				return Optional.empty();
			}
			else {
				statusTag = String.valueOf(response.getStatusCode().value());
				log.warn("API call returned non-200, endpoint={}, status={}, durationMs={}", endpoint,
						response.getStatusCode().value(), durationMs);
				return Optional.empty();
			}

		}
		catch (HttpClientErrorException e) {
			statusTag = String.valueOf(e.getStatusCode().value());
			handleHttpClientError(endpoint, e);

			if (e.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
				log.warn("Rate limit hit, not treated as breaker failure, endpoint={}", endpoint);
				return Optional.empty();
			}
			throw e;

		}
		catch (ResourceAccessException e) {
			statusTag = "network_error";
			log.error("Network error, endpoint={}, reason={}", endpoint, e.toString());
			throw e;
		}
		catch (Exception e) {
			statusTag = "unexpected_error";
			log.error("Unexpected error, endpoint={}, reason={}", endpoint, e.toString());
			throw new RuntimeException("API call failed", e);
		}
		finally {
			long durationNs = sample.stop(Timer.builder("opendota.api.call")
				.tag("method", "GET")
				.tag("endpoint", endpoint)
				.tag("status", statusTag)
				.register(meterRegistry));
			log.debug("API call timing, endpoint={}, status={}, durationMs={}, responseSize={}", endpoint, statusTag,
					durationNs, responseSize);
		}
	}

	// ----------------
	// Statistics
	// ----------------

	public ApiServiceStatistics getStatistics() {
		return ApiServiceStatistics.builder()
			.serviceName(SERVICE_NAME)
			.circuitBreakerStatus(circuitBreakerService.getStatus(SERVICE_NAME))
			.rateLimitStatus(openDotaRateLimitingService.getStatus())
			.baseUrl(baseUrl)
			.readTimeoutMs(readTimeoutMs)
			.connectTimeoutMs(connectTimeoutMs)
			.timestamp(Instant.now())
			.build();
	}

	@Override
	public Health health() {
		if (!healthCheckEnabled) {
			return Health.up().withDetail("healthCheck", "disabled").build();
		}
		try {
			Health.Builder builder = Health.up();
			builder.withDetail("circuitBreaker", circuitBreakerService.getStatus(SERVICE_NAME));
			builder.withDetail("rateLimiting", openDotaRateLimitingService.getStatus());

			if (circuitBreakerService.getStatus(SERVICE_NAME)
				.state() != CircuitBreakerService.CircuitBreakerState.OPEN) {
				performHealthCheck(builder);
			}
			return builder.build();
		}
		catch (Exception e) {
			log.error("Health check failed", e);
			return Health.down().withDetail("error", e.getMessage()).build();
		}
	}

	private void handleHttpClientError(String endpoint, HttpClientErrorException e) {
		HttpStatusCode status = e.getStatusCode();
		switch (status) {
			case HttpStatus.TOO_MANY_REQUESTS -> log.warn("Rate limited by upstream, endpoint={}", endpoint);
			case HttpStatus.NOT_FOUND -> log.warn("Resource not found, endpoint={}", endpoint);
			case HttpStatus.BAD_REQUEST -> log.warn("Bad request, endpoint={}", endpoint);
			case HttpStatus.UNAUTHORIZED, HttpStatus.FORBIDDEN -> log.error("Auth error, endpoint={}", endpoint);
			default -> log.error("HTTP error, endpoint={}", endpoint);
		}
	}

	private Optional<JsonNode> handleFallback(String endpoint) {
		log.warn("Fallback used, endpoint={}", endpoint);
		return Optional.empty();
	}

	private void performHealthCheck(Health.Builder builder) {
		try {
			Optional<JsonNode> response = performApiCall("/constants/heroes");
			builder.withDetail("apiCheck", response.isPresent() ? "successful" : "failed - no response");
		}
		catch (Exception e) {
			builder.withDetail("apiCheck", "failed - " + e.getMessage());
		}
	}

	@Builder
	public record ApiServiceStatistics(String serviceName,
			CircuitBreakerService.CircuitBreakerStatus circuitBreakerStatus,
			OpenDotaRateLimitingService.RateLimitStatus rateLimitStatus, String baseUrl, long readTimeoutMs,
			long connectTimeoutMs, Instant timestamp) {

	}

}
