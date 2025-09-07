package com.abe.gg_stats.controller;

import com.abe.gg_stats.dto.response.ActionResponse;
import com.abe.gg_stats.service.MetricsAdminService;
import com.abe.gg_stats.service.MetricsQueryService;
import io.micrometer.core.annotation.Timed;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/monitoring/metrics")
public class MetricsController {

	private final MetricsQueryService metricsQueryService;

	private final MetricsAdminService metricsAdminService;

	public MetricsController(MetricsQueryService metricsQueryService, MetricsAdminService metricsAdminService) {
		this.metricsQueryService = metricsQueryService;
		this.metricsAdminService = metricsAdminService;
	}

	@GetMapping
	@Timed(description = "Performance metrics retrieval")
	public ResponseEntity<Map<String, Object>> getPerformanceMetrics() {
		return metricsQueryService.getPerformanceMetrics();
	}

	@PostMapping("/reset")
	@Timed(description = "Reset performance metrics")
	public ResponseEntity<ActionResponse> resetPerformanceMetrics() {
		return metricsAdminService.resetPerformanceMetrics();
	}

}
