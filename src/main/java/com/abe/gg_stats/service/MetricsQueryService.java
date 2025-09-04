package com.abe.gg_stats.service;

import com.abe.gg_stats.util.LoggingUtils;
import io.micrometer.core.annotation.Timed;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class MetricsQueryService {

	private final MetricsService metricsService;

	public MetricsQueryService(MetricsService metricsService) {
		this.metricsService = metricsService;
	}

	@Timed(description = "Performance metrics retrieval")
	public ResponseEntity<Map<String, Object>> getPerformanceMetrics() {
		Map<String, Object> metrics = metricsService.getPerformanceMetrics();
		LoggingUtils.logDebug("Retrieved performance metrics", () -> "metricCount=" + metrics.size());
		return ResponseEntity.ok(metrics);
	}
}


