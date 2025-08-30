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
import jakarta.annotation.PostConstruct;
import java.time.Duration;
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
public class OpenDotaApiService implements HealthIndicator {

	private static final String SERVICE_NAME = "opendota_api";

	private final RestTemplate restTemplate;

	private final RateLimitingService rateLimitingService;

	private final CircuitBreakerService circuitBreakerService;

	private final ObjectMapper objectMapper;

	private final ServiceLogger serviceLogger;

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
		serviceLogger.logServiceStart("OpenDotaApiService", "OpenDota API service initialization");
		serviceLogger.logServiceSuccess("OpenDotaApiService", "OpenDota API service initialized", "baseUrl=" + baseUrl,
				"readTimeout=" + readTimeoutMs + "ms", "connectTimeout=" + connectTimeoutMs + "ms");
	}
	// ServiceLogger provides the service name automatically

	/**
	 * Makes a synchronous API call with custom timeout
	 */
	public Optional<JsonNode> makeApiCall(String endpoint) {
		try {
			return circuitBreakerService.executeWithCircuitBreaker(SERVICE_NAME, () -> performApiCall(endpoint),
					() -> handleFallback(endpoint));
		}
		catch (CircuitBreakerOpenException e) {
			LoggingUtils.logWarning("Circuit breaker prevented API call", "endpoint=" + endpoint,
					"reason=" + e.getMessage());
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

		try {
			// Check rate limit first
			RateLimitingService.RateLimitResult rateLimitResult = rateLimitingService.tryAcquirePermit(endpoint);

			if (!rateLimitResult.allowed()) {
				LoggingUtils.logWarning("Rate limit exceeded", "endpoint=" + endpoint,
						"reason=" + rateLimitResult.reason(), "resetTime=" + rateLimitResult.resetTimeMs() + "ms",
						"correlationId=" + correlationId);
				return Optional.empty();
			}

			try (LoggingUtils.AutoCloseableStopWatch _ = LoggingUtils
				.createStopWatch("opendota_api_call_" + endpoint.replaceAll("/", "_"))) {

				String url = baseUrl + endpoint;
				LoggingUtils.logApiCall(endpoint, "GET");

				Instant startTime = Instant.now();
				ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
				long responseTime = Duration.between(startTime, Instant.now()).toMillis();

				if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
					LoggingUtils.logApiResponse(endpoint, response.getStatusCode().value(), responseTime,
							response.getBody().length());

					return Optional.of(objectMapper.readTree(response.getBody()));

				}
				else if (response.getStatusCode() == HttpStatus.OK) {
					LoggingUtils.logWarning("API call successful but response body is null", "endpoint=" + endpoint);
					return Optional.empty();
				}
				else {
					LoggingUtils.logWarning("API call returned non-200 status", "endpoint=" + endpoint,
							"status=" + response.getStatusCode().value());
					return Optional.empty();
				}

			}
		}
		catch (HttpClientErrorException e) {
			handleHttpClientError(endpoint, e);

			// Don't treat rate limiting as a circuit breaker failure
			if (e.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
				LoggingUtils.logWarning("Rate limit hit - not counting as circuit breaker failure",
						"endpoint=" + endpoint);
				return Optional.empty(); // Return empty instead of throwing
			}

			throw e; // Re-throw for circuit breaker to handle other errors

		}
		catch (ResourceAccessException e) {
			LoggingUtils.logOperationFailure("opendota_api_call", "Network error for endpoint: " + endpoint, e);
			throw e; // Re-throw for retry and circuit breaker

		}
		catch (Exception e) {
			LoggingUtils.logOperationFailure("opendota_api_call", "Unexpected error for endpoint: " + endpoint, e);
			throw new RuntimeException("API call failed", e);
		}
		finally {
			// Don't clear context here - let the job-level listener handle context
			// clearing
			// MDCLoggingContext.clearContext(); // REMOVED: This was causing context loss
			// between heroes
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
	 * Gets comprehensive service statistics
	 */
	public ApiServiceStatistics getStatistics() {
		CircuitBreakerService.CircuitBreakerStatus cbStatus = circuitBreakerService.getStatus(SERVICE_NAME);
		RateLimitingService.RateLimitStatus rlStatus = rateLimitingService.getStatus();

		return ApiServiceStatistics.builder()
			.serviceName(SERVICE_NAME)
			.circuitBreakerStatus(cbStatus)
			.rateLimitStatus(rlStatus)
			.baseUrl(baseUrl)
			.readTimeoutMs(readTimeoutMs)
			.connectTimeoutMs(connectTimeoutMs)
			.timestamp(Instant.now())
			.build();
	}

	private void handleHttpClientError(String endpoint, HttpClientErrorException e) {
		HttpStatusCode status = e.getStatusCode();

		switch (status) {
			case TOO_MANY_REQUESTS:
				if (e.getResponseHeaders() != null) {
					LoggingUtils.logWarning("Rate limited by OpenDota API", "endpoint=" + endpoint,
							"retryAfter=" + e.getResponseHeaders().getFirst("Retry-After"));
				}
				break;

			case NOT_FOUND:
				LoggingUtils.logWarning("Resource not found", "endpoint=" + endpoint);
				break;

			case BAD_REQUEST:
				LoggingUtils.logWarning("Bad request to API", "endpoint=" + endpoint,
						"response=" + e.getResponseBodyAsString());
				break;

			case UNAUTHORIZED:
			case FORBIDDEN:
				LoggingUtils.logOperationFailure("opendota_api_call", "Authentication/authorization error", e);
				break;

			default:
				LoggingUtils.logOperationFailure("opendota_api_call", "HTTP error: " + status, e);
		}
	}

	private Optional<JsonNode> handleFallback(String endpoint) {
		LoggingUtils.logWarning("Using fallback for API call", "endpoint=" + endpoint);
		// TODO: Implement fallback logic - could be cached data, default values, or
		// alternative data source
		// For now, return empty as fallback is not yet implemented
		return Optional.empty();
	}

	private void performHealthCheck(Health.Builder healthBuilder) {
		try {
			// Use a lightweight endpoint for health check
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
	 * Statistics container for monitoring
	 */
	@Builder
	public record ApiServiceStatistics(String serviceName,
			CircuitBreakerService.CircuitBreakerStatus circuitBreakerStatus,
			RateLimitingService.RateLimitStatus rateLimitStatus, String baseUrl, long readTimeoutMs,
			long connectTimeoutMs, Instant timestamp) {

	}

}
