package com.abe.gg_stats.service;

import com.abe.gg_stats.util.LoggingUtils;
import io.micrometer.core.annotation.Timed;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class CircuitBreakerQueryService {

	private final CircuitBreakerService circuitBreakerService;

	public CircuitBreakerQueryService(CircuitBreakerService circuitBreakerService) {
		this.circuitBreakerService = circuitBreakerService;
	}

	@Timed(description = "Circuit breaker status check")
	public ResponseEntity<Map<String, CircuitBreakerService.CircuitBreakerStatus>> getAllStatuses() {
		Map<String, CircuitBreakerService.CircuitBreakerStatus> statuses = circuitBreakerService.getAllStatuses();
		LoggingUtils.logDebug("Retrieved circuit breaker statuses", () -> "serviceCount=" + statuses.size());
		return ResponseEntity.ok(statuses);
	}

}
