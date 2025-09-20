package com.abe.gg_stats.service.circuit_breaker;

import com.abe.gg_stats.dto.response.ActionResponse;
import java.time.Instant;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class CircuitBreakerQueryService {

	private final CircuitBreakerService circuitBreakerService;

	@Autowired
	public CircuitBreakerQueryService(CircuitBreakerService circuitBreakerService) {
		this.circuitBreakerService = circuitBreakerService;
	}

	public ResponseEntity<ActionResponse> openCircuitBreaker(String serviceName, String reason) {
		try {
			circuitBreakerService.openCircuitBreaker(serviceName, reason);
			ActionResponse response = ActionResponse.success("Circuit breaker opened for service: " + serviceName,
					Map.of("service", serviceName, "reason", reason));
			return ResponseEntity.ok(response);
		}
		catch (Exception e) {
			return ResponseEntity.internalServerError()
				.body(ActionResponse.error("Failed to open circuit breaker: " + e.getMessage()));
		}
	}

	public ResponseEntity<ActionResponse> closeCircuitBreaker(String serviceName) {
		try {
			circuitBreakerService.closeCircuitBreaker(serviceName);
			ActionResponse response = ActionResponse.success("Circuit breaker closed for service: " + serviceName,
					Map.of("service", serviceName));
			return ResponseEntity.ok(response);
		}
		catch (Exception e) {
			return ResponseEntity.internalServerError()
				.body(ActionResponse.error("Failed to close circuit breaker: " + e.getMessage()));
		}
	}

	public ResponseEntity<ActionResponse> resetCircuitBreakerMetrics(String serviceName) {
		try {
			circuitBreakerService.resetMetrics(serviceName);
			ActionResponse response = ActionResponse.success(
					"Circuit breaker metrics reset for service: " + serviceName,
					Map.of("service", serviceName, "resetTime", Instant.now().toString()));
			return ResponseEntity.ok(response);
		}
		catch (Exception e) {
			return ResponseEntity.internalServerError()
				.body(ActionResponse.error("Failed to reset metrics: " + e.getMessage()));
		}
	}

	public ResponseEntity<Map<String, CircuitBreakerService.CircuitBreakerStatus>> getAllStatuses() {
		Map<String, CircuitBreakerService.CircuitBreakerStatus> statuses = circuitBreakerService.getAllStatuses();
		return ResponseEntity.ok(statuses);
	}

}
