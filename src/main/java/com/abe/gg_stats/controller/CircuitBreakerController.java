package com.abe.gg_stats.controller;

import com.abe.gg_stats.dto.response.ActionResponse;
import com.abe.gg_stats.service.circuit_breaker.CircuitBreakerQueryService;
import com.abe.gg_stats.service.circuit_breaker.CircuitBreakerService;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@ResponseBody
@RequestMapping("/api/monitoring/circuit-breakers")
class CircuitBreakerController {

	private final CircuitBreakerQueryService circuitBreakerQueryService;

	@Autowired
	CircuitBreakerController(CircuitBreakerQueryService circuitBreakerQueryService) {
		this.circuitBreakerQueryService = circuitBreakerQueryService;
	}

	@GetMapping()
	ResponseEntity<Map<String, CircuitBreakerService.CircuitBreakerStatus>> getCircuitBreakerStatuses() {
		return circuitBreakerQueryService.getAllStatuses();
	}

	@PostMapping("/{serviceName}/open")
	ResponseEntity<ActionResponse> openCircuitBreaker(@PathVariable String serviceName,
			@RequestParam(defaultValue = "Manual override") String reason) {
		return circuitBreakerQueryService.openCircuitBreaker(serviceName, reason);
	}

	@PostMapping("/{serviceName}/close")
	ResponseEntity<ActionResponse> closeCircuitBreaker(@PathVariable String serviceName) {
		return circuitBreakerQueryService.closeCircuitBreaker(serviceName);
	}

	@PostMapping("/{serviceName}/reset")
	ResponseEntity<ActionResponse> resetCircuitBreakerMetrics(@PathVariable String serviceName) {
		return circuitBreakerQueryService.resetCircuitBreakerMetrics(serviceName);
	}

}
