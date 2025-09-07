package com.abe.gg_stats.service;

import com.abe.gg_stats.dto.response.ActionResponse;
import io.micrometer.core.annotation.Timed;
import java.time.Instant;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class CircuitBreakerAdminService {

	private final CircuitBreakerService circuitBreakerService;

	private final ServiceLogger serviceLogger;

	public CircuitBreakerAdminService(CircuitBreakerService circuitBreakerService, ServiceLogger serviceLogger) {
		this.circuitBreakerService = circuitBreakerService;
		this.serviceLogger = serviceLogger;
	}

	@Timed(description = "Force open circuit breaker")
	public ResponseEntity<ActionResponse> openCircuitBreaker(String serviceName, String reason) {
		serviceLogger.logServiceStart("CircuitBreakerAdminService", "Force opening circuit breaker",
				"service=" + serviceName, "reason=" + reason);
		try {
			circuitBreakerService.openCircuitBreaker(serviceName, reason);
			ActionResponse response = ActionResponse.success("Circuit breaker opened for service: " + serviceName,
					Map.of("service", serviceName, "reason", reason));
			serviceLogger.logServiceSuccess("CircuitBreakerAdminService", "Circuit breaker force opened",
					"service=" + serviceName);
			return ResponseEntity.ok(response);
		}
		catch (Exception e) {
			serviceLogger.logServiceFailure("force_open_circuit_breaker", "Failed to open circuit breaker", e);
			return ResponseEntity.internalServerError()
				.body(ActionResponse.error("Failed to open circuit breaker: " + e.getMessage()));
		}
	}

	@Timed(description = "Force close circuit breaker")
	public ResponseEntity<ActionResponse> closeCircuitBreaker(String serviceName) {
		serviceLogger.logServiceStart("CircuitBreakerAdminService", "Force closing circuit breaker",
				"service=" + serviceName);
		try {
			circuitBreakerService.closeCircuitBreaker(serviceName);
			ActionResponse response = ActionResponse.success("Circuit breaker closed for service: " + serviceName,
					Map.of("service", serviceName));
			serviceLogger.logServiceSuccess("CircuitBreakerAdminService", "Circuit breaker force closed",
					"service=" + serviceName);
			return ResponseEntity.ok(response);
		}
		catch (Exception e) {
			serviceLogger.logServiceFailure("force_close_circuit_breaker", "Failed to close circuit breaker", e);
			return ResponseEntity.internalServerError()
				.body(ActionResponse.error("Failed to close circuit breaker: " + e.getMessage()));
		}
	}

	@Timed(description = "Reset circuit breaker metrics")
	public ResponseEntity<ActionResponse> resetCircuitBreakerMetrics(String serviceName) {
		serviceLogger.logServiceStart("CircuitBreakerAdminService", "Resetting circuit breaker metrics",
				"service=" + serviceName);
		try {
			circuitBreakerService.resetMetrics(serviceName);
			ActionResponse response = ActionResponse.success(
					"Circuit breaker metrics reset for service: " + serviceName,
					Map.of("service", serviceName, "resetTime", Instant.now().toString()));
			serviceLogger.logServiceSuccess("CircuitBreakerAdminService", "Circuit breaker metrics reset",
					"service=" + serviceName);
			return ResponseEntity.ok(response);
		}
		catch (Exception e) {
			serviceLogger.logServiceFailure("reset_circuit_breaker_metrics", "Failed to reset circuit breaker metrics",
					e);
			return ResponseEntity.internalServerError()
				.body(ActionResponse.error("Failed to reset metrics: " + e.getMessage()));
		}
	}

}
