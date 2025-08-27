package com.abe.gg_stats.controller;

import com.abe.gg_stats.service.CircuitBreakerService;
import com.abe.gg_stats.service.OpenDotaApiService;
import com.abe.gg_stats.service.RateLimitingService;
import com.abe.gg_stats.util.LoggingUtils;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;

/**
 * Enterprise monitoring and management controller providing comprehensive system
 * observability and control capabilities.
 *
 * Features: - Real-time system health monitoring - Circuit breaker status and control -
 * Rate limiting metrics and management - Performance metrics and logging statistics -
 * Administrative operations for system management
 */
@RestController
@RequestMapping("/api/monitoring")
@RequiredArgsConstructor
@Slf4j
public class MonitoringController {

	private final OpenDotaApiService openDotaApiService;

	private final CircuitBreakerService circuitBreakerService;

	private final RateLimitingService rateLimitingService;

	/**
	 * Get comprehensive system health status
	 */
	@GetMapping("/health")
	@Timed(description = "System health check")
	public ResponseEntity<SystemHealthResponse> getSystemHealth() {
		try (LoggingUtils.AutoCloseableStopWatch stopWatch = LoggingUtils.createStopWatch("system_health_check")) {

			Health apiHealth = openDotaApiService.health();
			OpenDotaApiService.ApiServiceStatistics stats = openDotaApiService.getStatistics();

			SystemHealthResponse response = SystemHealthResponse.builder()
				.timestamp(Instant.now())
				.overallStatus(determineOverallStatus(apiHealth))
				.apiHealth(apiHealth)
				.circuitBreakerStatus(stats.circuitBreakerStatus())
				.rateLimitStatus(stats.rateLimitStatus())
				.performanceMetrics(LoggingUtils.getPerformanceMetrics())
				.build();

			LoggingUtils.logOperationSuccess("System health check completed",
					"overallStatus=" + response.getOverallStatus());

			return ResponseEntity.ok(response);
		}
	}

	/**
	 * Get detailed circuit breaker status for all services
	 */
	@GetMapping("/circuit-breakers")
	@Timed(description = "Circuit breaker status check")
	public ResponseEntity<Map<String, CircuitBreakerService.CircuitBreakerStatus>> getCircuitBreakerStatuses() {
		Map<String, CircuitBreakerService.CircuitBreakerStatus> statuses = circuitBreakerService.getAllStatuses();

		LoggingUtils.logDebug("Retrieved circuit breaker statuses", () -> "serviceCount=" + statuses.size());

		return ResponseEntity.ok(statuses);
	}

	/**
	 * Get rate limiting status and metrics
	 */
	@GetMapping("/rate-limits")
	public ResponseEntity<RateLimitingService.RateLimitStatus> getRateLimitStatus() {
		RateLimitingService.RateLimitStatus status = rateLimitingService.getStatus();

		LoggingUtils.logDebug("Retrieved rate limit status", () -> "availableTokens=" + status.availableTokens(),
				() -> "remainingDaily=" + status.remainingDailyRequests());

		return ResponseEntity.ok(status);
	}

	/**
	 * Get performance metrics and logging statistics
	 */
	@GetMapping("/metrics")
	@Timed(description = "Performance metrics retrieval")
	public ResponseEntity<Map<String, Object>> getPerformanceMetrics() {
		Map<String, Object> metrics = LoggingUtils.getPerformanceMetrics();

		LoggingUtils.logDebug("Retrieved performance metrics", () -> "metricCount=" + metrics.size());

		return ResponseEntity.ok(metrics);
	}

	/**
	 * Administrative: Force open a circuit breaker
	 */
	@PostMapping("/circuit-breakers/{serviceName}/open")
	@Timed(description = "Force open circuit breaker")
	public ResponseEntity<ActionResponse> openCircuitBreaker(@PathVariable String serviceName,
			@RequestParam(defaultValue = "Manual override") String reason) {

		LoggingUtils.logOperationStart("Force opening circuit breaker", "service=" + serviceName, "reason=" + reason);

		try {
			circuitBreakerService.openCircuitBreaker(serviceName, reason);

			ActionResponse response = ActionResponse.success("Circuit breaker opened for service: " + serviceName,
					Map.of("service", serviceName, "reason", reason));

			LoggingUtils.logOperationSuccess("Circuit breaker force opened", "service=" + serviceName);

			return ResponseEntity.ok(response);

		}
		catch (Exception e) {
			LoggingUtils.logOperationFailure("force_open_circuit_breaker", "Failed to open circuit breaker", e);

			return ResponseEntity.internalServerError()
				.body(ActionResponse.error("Failed to open circuit breaker: " + e.getMessage()));
		}
	}

	/**
	 * Administrative: Force close a circuit breaker
	 */
	@PostMapping("/circuit-breakers/{serviceName}/close")
	@Timed(description = "Force close circuit breaker")
	public ResponseEntity<ActionResponse> closeCircuitBreaker(@PathVariable String serviceName) {

		LoggingUtils.logOperationStart("Force closing circuit breaker", "service=" + serviceName);

		try {
			circuitBreakerService.closeCircuitBreaker(serviceName);

			ActionResponse response = ActionResponse.success("Circuit breaker closed for service: " + serviceName,
					Map.of("service", serviceName));

			LoggingUtils.logOperationSuccess("Circuit breaker force closed", "service=" + serviceName);

			return ResponseEntity.ok(response);

		}
		catch (Exception e) {
			LoggingUtils.logOperationFailure("force_close_circuit_breaker", "Failed to close circuit breaker", e);

			return ResponseEntity.internalServerError()
				.body(ActionResponse.error("Failed to close circuit breaker: " + e.getMessage()));
		}
	}

	/**
	 * Administrative: Reset circuit breaker metrics
	 */
	@PostMapping("/circuit-breakers/{serviceName}/reset")
	@Timed(description = "Reset circuit breaker metrics")
	public ResponseEntity<ActionResponse> resetCircuitBreakerMetrics(@PathVariable String serviceName) {

		LoggingUtils.logOperationStart("Resetting circuit breaker metrics", "service=" + serviceName);

		try {
			circuitBreakerService.resetMetrics(serviceName);

			ActionResponse response = ActionResponse.success(
					"Circuit breaker metrics reset for service: " + serviceName,
					Map.of("service", serviceName, "resetTime", Instant.now().toString()));

			LoggingUtils.logOperationSuccess("Circuit breaker metrics reset", "service=" + serviceName);

			return ResponseEntity.ok(response);

		}
		catch (Exception e) {
			LoggingUtils.logOperationFailure("reset_circuit_breaker_metrics", "Failed to reset circuit breaker metrics",
					e);

			return ResponseEntity.internalServerError()
				.body(ActionResponse.error("Failed to reset metrics: " + e.getMessage()));
		}
	}

	/**
	 * Administrative: Reset performance metrics
	 */
	@PostMapping("/metrics/reset")
	@Timed(description = "Reset performance metrics")
	public ResponseEntity<ActionResponse> resetPerformanceMetrics() {

		LoggingUtils.logOperationStart("Resetting performance metrics");

		try {
			LoggingUtils.resetMetrics();

			ActionResponse response = ActionResponse.success("Performance metrics reset successfully",
					Map.of("resetTime", Instant.now().toString()));

			LoggingUtils.logOperationSuccess("Performance metrics reset");

			return ResponseEntity.ok(response);

		}
		catch (Exception e) {
			LoggingUtils.logOperationFailure("reset_performance_metrics", "Failed to reset performance metrics", e);

			return ResponseEntity.internalServerError()
				.body(ActionResponse.error("Failed to reset metrics: " + e.getMessage()));
		}
	}

	/**
	 * Test endpoint for validating API connectivity
	 */
	@GetMapping("/test/api-connectivity")
	@Timed(description = "Test API connectivity")
	public ResponseEntity<ActionResponse> testApiConnectivity() {

		LoggingUtils.logOperationStart("Testing API connectivity");

		try {
			// Test with a lightweight API call
			var result = openDotaApiService.getHeroes();

			if (result.isPresent()) {
				ActionResponse response = ActionResponse.success("API connectivity test successful", Map.of("testTime",
						Instant.now().toString(), "responseReceived", true, "heroesCount", result.get().size()));

				LoggingUtils.logOperationSuccess("API connectivity test passed");
				return ResponseEntity.ok(response);

			}
			else {
				ActionResponse response = ActionResponse.error("API connectivity test failed - no response received");

				LoggingUtils.logOperationFailure("api_connectivity_test", "No response received from API", null);
				return ResponseEntity.status(503).body(response);
			}

		}
		catch (Exception e) {
			LoggingUtils.logOperationFailure("api_connectivity_test", "API connectivity test failed", e);

			ActionResponse response = ActionResponse.error("API connectivity test failed: " + e.getMessage());

			return ResponseEntity.status(503).body(response);
		}
	}

	// Helper methods

	private String determineOverallStatus(Health apiHealth) {
		if (apiHealth.getStatus().equals(org.springframework.boot.actuate.health.Status.UP)) {
			return "HEALTHY";
		}
		else if (apiHealth.getStatus().equals(org.springframework.boot.actuate.health.Status.DOWN)) {
			return "UNHEALTHY";
		}
		else {
			return "DEGRADED";
		}
	}

	// Response DTOs

	public static class SystemHealthResponse {

		private final Instant timestamp;

		private final String overallStatus;

		private final Health apiHealth;

		private final CircuitBreakerService.CircuitBreakerStatus circuitBreakerStatus;

		private final RateLimitingService.RateLimitStatus rateLimitStatus;

		private final Map<String, Object> performanceMetrics;

		private SystemHealthResponse(Builder builder) {
			this.timestamp = builder.timestamp;
			this.overallStatus = builder.overallStatus;
			this.apiHealth = builder.apiHealth;
			this.circuitBreakerStatus = builder.circuitBreakerStatus;
			this.rateLimitStatus = builder.rateLimitStatus;
			this.performanceMetrics = builder.performanceMetrics;
		}

		public static Builder builder() {
			return new Builder();
		}

		// Getters
		public Instant getTimestamp() {
			return timestamp;
		}

		public String getOverallStatus() {
			return overallStatus;
		}

		public Health getApiHealth() {
			return apiHealth;
		}

		public CircuitBreakerService.CircuitBreakerStatus getCircuitBreakerStatus() {
			return circuitBreakerStatus;
		}

		public RateLimitingService.RateLimitStatus getRateLimitStatus() {
			return rateLimitStatus;
		}

		public Map<String, Object> getPerformanceMetrics() {
			return performanceMetrics;
		}

		public static class Builder {

			private Instant timestamp;

			private String overallStatus;

			private Health apiHealth;

			private CircuitBreakerService.CircuitBreakerStatus circuitBreakerStatus;

			private RateLimitingService.RateLimitStatus rateLimitStatus;

			private Map<String, Object> performanceMetrics;

			public Builder timestamp(Instant timestamp) {
				this.timestamp = timestamp;
				return this;
			}

			public Builder overallStatus(String overallStatus) {
				this.overallStatus = overallStatus;
				return this;
			}

			public Builder apiHealth(Health apiHealth) {
				this.apiHealth = apiHealth;
				return this;
			}

			public Builder circuitBreakerStatus(CircuitBreakerService.CircuitBreakerStatus circuitBreakerStatus) {
				this.circuitBreakerStatus = circuitBreakerStatus;
				return this;
			}

			public Builder rateLimitStatus(RateLimitingService.RateLimitStatus rateLimitStatus) {
				this.rateLimitStatus = rateLimitStatus;
				return this;
			}

			public Builder performanceMetrics(Map<String, Object> performanceMetrics) {
				this.performanceMetrics = performanceMetrics;
				return this;
			}

			public SystemHealthResponse build() {
				return new SystemHealthResponse(this);
			}

		}

	}

	public static class ActionResponse {

		private final boolean success;

		private final String message;

		private final Map<String, Object> details;

		private final Instant timestamp;

		private ActionResponse(boolean success, String message, Map<String, Object> details) {
			this.success = success;
			this.message = message;
			this.details = details;
			this.timestamp = Instant.now();
		}

		public static ActionResponse success(String message) {
			return new ActionResponse(true, message, Map.of());
		}

		public static ActionResponse success(String message, Map<String, Object> details) {
			return new ActionResponse(true, message, details);
		}

		public static ActionResponse error(String message) {
			return new ActionResponse(false, message, Map.of());
		}

		public static ActionResponse error(String message, Map<String, Object> details) {
			return new ActionResponse(false, message, details);
		}

		// Getters
		public boolean isSuccess() {
			return success;
		}

		public String getMessage() {
			return message;
		}

		public Map<String, Object> getDetails() {
			return details;
		}

		public Instant getTimestamp() {
			return timestamp;
		}

	}

}