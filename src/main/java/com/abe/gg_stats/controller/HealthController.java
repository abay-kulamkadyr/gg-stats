package com.abe.gg_stats.controller;

import com.abe.gg_stats.dto.response.SystemHealthResponse;
import com.abe.gg_stats.service.SystemHealthService;
import io.micrometer.core.annotation.Timed;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/monitoring")
public class HealthController {

	private final SystemHealthService systemHealthService;

	public HealthController(SystemHealthService systemHealthService) {
		this.systemHealthService = systemHealthService;
	}

	@GetMapping("/health")
	@Timed(description = "System health check")
	public ResponseEntity<SystemHealthResponse> getSystemHealth() {
		return systemHealthService.getSystemHealth();
	}
}


