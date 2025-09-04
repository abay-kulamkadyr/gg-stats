package com.abe.gg_stats.controller;

import com.abe.gg_stats.dto.response.ActionResponse;
import com.abe.gg_stats.service.CircuitBreakerAdminService;
import com.abe.gg_stats.service.CircuitBreakerQueryService;
import com.abe.gg_stats.service.CircuitBreakerService;
import io.micrometer.core.annotation.Timed;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/monitoring/circuit-breakers")
public class CircuitBreakerController {

	private final CircuitBreakerQueryService circuitBreakerQueryService;

	private final CircuitBreakerAdminService circuitBreakerAdminService;

	public CircuitBreakerController(CircuitBreakerQueryService circuitBreakerQueryService,
			CircuitBreakerAdminService circuitBreakerAdminService) {
		this.circuitBreakerQueryService = circuitBreakerQueryService;
		this.circuitBreakerAdminService = circuitBreakerAdminService;
	}

	@GetMapping()
	@Timed(description = "Circuit breaker status check")
	public ResponseEntity<Map<String, CircuitBreakerService.CircuitBreakerStatus>> getCircuitBreakerStatuses() {
		return circuitBreakerQueryService.getAllStatuses();
	}

	@PostMapping("/{serviceName}/open")
	@Timed(description = "Force open circuit breaker")
	public ResponseEntity<ActionResponse> openCircuitBreaker(@PathVariable String serviceName,
			@RequestParam(defaultValue = "Manual override") String reason) {
		return circuitBreakerAdminService.openCircuitBreaker(serviceName, reason);
	}

	@PostMapping("/{serviceName}/close")
	@Timed(description = "Force close circuit breaker")
	public ResponseEntity<ActionResponse> closeCircuitBreaker(@PathVariable String serviceName) {
		return circuitBreakerAdminService.closeCircuitBreaker(serviceName);
	}

	@PostMapping("/{serviceName}/reset")
	@Timed(description = "Reset circuit breaker metrics")
	public ResponseEntity<ActionResponse> resetCircuitBreakerMetrics(@PathVariable String serviceName) {
		return circuitBreakerAdminService.resetCircuitBreakerMetrics(serviceName);
	}
}


