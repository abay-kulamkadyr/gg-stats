package com.abe.gg_stats.service;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.TOO_MANY_REQUESTS;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

import com.abe.gg_stats.exception.CircuitBreakerOpenException;
import com.abe.gg_stats.util.LoggingConstants;
import com.abe.gg_stats.util.LoggingUtils;
import com.abe.gg_stats.util.MDCLoggingContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import jakarta.annotation.PostConstruct;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import static net.logstash.logback.argument.StructuredArguments.kv;

/**
 * OpenDota API client with comprehensive resilience patterns.
 * <p>
 * Features: - Rate limiting with token bucket algorithm - Circuit breaker pattern for
 * fault tolerance - Comprehensive monitoring and health checks - Async execution support
 * - Enhanced error handling with proper classification - Structured logging with
 * performance metrics - Configuration validation
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OpenDotaApiService implements HealthIndicator {

	private static final String SERVICE_NAME = "opendota_api";

	private final RestTemplate restTemplate;

	private final RateLimitingService rateLimitingService;

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

	@PostConstruct
	public void initialize() {
		log.info("OpenDotaApiService initialized", kv("baseUrl", baseUrl), kv("readTimeoutMs", readTimeoutMs),
				kv("connectTimeoutMs", connectTimeoutMs));
	}
	// Initialization complete

	/**
	 * Makes a synchronous API call with custom timeout
	 */
	public Optional<JsonNode> makeApiCall(String endpoint) {
		try {
			return circuitBreakerService.executeWithCircuitBreaker(SERVICE_NAME, () -> performApiCall(endpoint),
					() -> handleFallback(endpoint));
		}
		catch (CircuitBreakerOpenException e) {
			log.warn("Circuit breaker prevented API call", kv("endpoint", endpoint), kv("reason", e.getMessage()));
			return Optional.empty();
		}
	}

	private Optional<JsonNode> performApiCall(String endpoint) {
		// Set up API call context - inherit existing context if available
		String existingCorrelationId = MDCLoggingContext.getCurrentCorrelationId();
		String correlationId = existingCorrelationId != null ? existingCorrelationId
				: MDCLoggingContext.setApiContext(endpoint, "GET");

		// Update context with API-specific information
		MDCLoggingContext.updateContext("apiEndpoint", endpoint);
		MDCLoggingContext.updateContext("operationType", LoggingConstants.OPERATION_TYPE_API_CALL);

		Timer.Sample sample = Timer.start(meterRegistry);
		String statusTag = "unknown";
		long responseSize = -1;
		long durationMs = -1;

		try {
			// Check rate limit first
			RateLimitingService.RateLimitResult rateLimitResult = rateLimitingService.tryAcquirePermit(endpoint);

			if (!rateLimitResult.allowed()) {
				statusTag = "rate_limited";
				log.warn("Rate limit exceeded", kv("endpoint", endpoint), kv("reason", rateLimitResult.reason()),
						kv("resetTimeMs", rateLimitResult.resetTimeMs()), kv("correlationId", correlationId));
				return Optional.empty();
			}

			String url = baseUrl + endpoint;
			Instant startTime = Instant.now();
			ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
			durationMs = java.time.Duration.between(startTime, Instant.now()).toMillis();

			if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
				statusTag = String.valueOf(response.getStatusCode().value());
				responseSize = response.getBody().length();
				log.info("OpenDota API call success", kv("endpoint", endpoint),
						kv("status", response.getStatusCode().value()), kv("durationMs", durationMs),
						kv("size", responseSize));
				return Optional.of(objectMapper.readTree(response.getBody()));

			}
			else if (response.getStatusCode() == HttpStatus.OK) {
				statusTag = String.valueOf(response.getStatusCode().value());
				log.warn("API call successful but response body is null", kv("endpoint", endpoint));
				return Optional.empty();
			}
			else {
				statusTag = String.valueOf(response.getStatusCode().value());
				log.warn("API call returned non-200 status", kv("endpoint", endpoint),
						kv("status", response.getStatusCode().value()), kv("durationMs", durationMs));
				return Optional.empty();
			}
		}
		catch (HttpClientErrorException e) {
			statusTag = String.valueOf(e.getStatusCode().value());
			handleHttpClientError(endpoint, e);

			// Don't treat rate limiting as a circuit breaker failure
			if (e.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
				log.warn("Rate limit hit - not counting as circuit breaker failure", kv("endpoint", endpoint));
				return Optional.empty(); // Return empty instead of throwing
			}

			throw e; // Re-throw for circuit breaker to handle other errors

		}
		catch (ResourceAccessException e) {
			statusTag = "network_error";
			log.error("Network error for endpoint", e);
			throw e; // Re-throw for retry and circuit breaker

		}
		catch (Exception e) {
			statusTag = "unexpected_error";
			log.error("Unexpected error for endpoint", e);
			throw new RuntimeException("API call failed", e);
		}

		finally {
			long durationNs = sample.stop(Timer.builder("opendota.api.call")
				.tag("method", "GET")
				.tag("endpoint", endpoint)
				.tag("status", statusTag)
				.register(meterRegistry));
			log.debug("OpenDota API call timing", kv("endpoint", endpoint), kv("status", statusTag),
					kv("durationNs", durationNs), kv("size", responseSize));
		}
	}

	public Optional<JsonNode> getHeroes() {
		return makeApiCall("/heroes");
	}

	public Optional<JsonNode> getProPlayers() {
		return makeApiCall("/proPlayers");
	}

	public Optional<JsonNode> getTeams() {
		return makeApiCall("/teams");
	}

	public Optional<JsonNode> getTeamsPage(int page) {
		if (page < 1)
			page = 1;
		if (page > 50)
			page = 50;
		return makeApiCall("/teams?page=" + page);
	}

	public Optional<JsonNode> getProMatchesPage(Long lessThanMatchId) {
		String endpoint = "/proMatches" + (lessThanMatchId != null ? ("?less_than_match_id=" + lessThanMatchId) : "");
		return makeApiCall(endpoint);
	}

	public Optional<JsonNode> getMatchDetail(long matchId) {
		return makeApiCall("/matches/" + matchId);
	}

	public Optional<JsonNode> getPatches() {
		return makeApiCall("/constants/patch");
	}

	public Optional<JsonNode> getPlayer(Long accountId) {
		if (accountId == null || accountId <= 0) {
			LoggingUtils.logWarning("Invalid account ID provided", "accountId=" + accountId);
			return Optional.empty();
		}
		return makeApiCall("/players/" + accountId);
	}

	public Optional<JsonNode> getHeroRanking(Integer heroId) {
		if (heroId == null || heroId <= 0) {
			LoggingUtils.logWarning("Invalid hero ID provided", "heroId=" + heroId);
			return Optional.empty();
		}
		return makeApiCall("/rankings?hero_id=" + heroId);
	}

	@Override
	public Health health() {
		if (!healthCheckEnabled) {
			return Health.up().withDetail("healthCheck", "disabled").build();
		}

		try {
			Health.Builder healthBuilder = Health.up();

			// Check circuit breaker status
			CircuitBreakerService.CircuitBreakerStatus cbStatus = circuitBreakerService.getStatus(SERVICE_NAME);
			healthBuilder.withDetail("circuitBreaker", Map.of("state", cbStatus.state().toString(), "successRate",
					cbStatus.successRate(), "totalCalls", cbStatus.totalCalls()));

			// Check rate limiting status
			RateLimitingService.RateLimitStatus rlStatus = rateLimitingService.getStatus();
			healthBuilder.withDetail("rateLimiting",
					Map.of("availableTokens", rlStatus.availableTokens(), "remainingDailyRequests",
							rlStatus.remainingDailyRequests(), "successRate", rlStatus.successRate()));

			// Perform a lightweight health check call
			if (cbStatus.state() != CircuitBreakerService.CircuitBreakerState.OPEN) {
				performHealthCheck(healthBuilder);
			}

			return healthBuilder.build();

		}
		catch (Exception e) {
			LoggingUtils.logOperationFailure("health_check", "Health check failed", e);
			return Health.down()
				.withDetail("error", e.getMessage())
				.withDetail("timestamp", Instant.now().toString())
				.build();
		}
	}

	/**
	 * Handle HTTP client errors in a centralized way for logging and classification.
	 */
	private void handleHttpClientError(String endpoint, HttpClientErrorException e) {
		HttpStatusCode status = e.getStatusCode();
		if (status == TOO_MANY_REQUESTS) {
			if (e.getResponseHeaders() != null) {
				log.warn("Rate limited by OpenDota API", kv("endpoint", endpoint),
						kv("retryAfter", e.getResponseHeaders().getFirst("Retry-After")));
			}
			return;
		}
		if (status == NOT_FOUND) {
			log.warn("Resource not found", kv("endpoint", endpoint));
			return;
		}
		if (status == BAD_REQUEST) {
			log.warn("Bad request to API", kv("endpoint", endpoint), kv("response", e.getResponseBodyAsString()));
			return;
		}
		if (status == UNAUTHORIZED || status == FORBIDDEN) {
			log.error("Authentication/authorization error", e);
			return;
		}
		log.error("HTTP error", e);
	}

	/**
	 * Fallback used by the circuit breaker wrapper when performApiCall fails.
	 */
	private Optional<JsonNode> handleFallback(String endpoint) {
		log.warn("Using fallback for API call", kv("endpoint", endpoint));
		return Optional.empty();
	}

	/**
	 * Lightweight upstream check used in the /health endpoint to avoid heavy calls.
	 */
	private void performHealthCheck(Health.Builder healthBuilder) {
		try {
			Optional<JsonNode> response = performApiCall("/constants/heroes");
			if (response.isPresent()) {
				healthBuilder.withDetail("apiCheck", "successful");
			}
			else {
				healthBuilder.withDetail("apiCheck", "failed - no response");
			}
		}
		catch (Exception e) {
			healthBuilder.withDetail("apiCheck", "failed - " + e.getMessage());
		}
	}

	/**
	 * Gets comprehensive service statistics
	 */
	@Builder
	public record ApiServiceStatistics(String serviceName,
			CircuitBreakerService.CircuitBreakerStatus circuitBreakerStatus,
			RateLimitingService.RateLimitStatus rateLimitStatus, String baseUrl, long readTimeoutMs,
			long connectTimeoutMs, Instant timestamp) {

	}

}
