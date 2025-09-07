package com.abe.gg_stats.service;

import com.abe.gg_stats.dto.response.ActionResponse;
import io.micrometer.core.annotation.Timed;
import java.time.Instant;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class MetricsAdminService {

	private final MetricsService metricsService;

	private final ServiceLogger serviceLogger;

	public MetricsAdminService(MetricsService metricsService, ServiceLogger serviceLogger) {
		this.metricsService = metricsService;
		this.serviceLogger = serviceLogger;
	}

	@Timed(description = "Reset performance metrics")
	public ResponseEntity<ActionResponse> resetPerformanceMetrics() {
		serviceLogger.logServiceStart("MetricsAdminService", "Resetting performance metrics");
		try {
			metricsService.resetMetrics();
			ActionResponse response = ActionResponse.success("Performance metrics reset successfully",
					Map.of("resetTime", Instant.now().toString()));
			serviceLogger.logServiceSuccess("MetricsAdminService", "Performance metrics reset");
			return ResponseEntity.ok(response);
		}
		catch (Exception e) {
			serviceLogger.logServiceFailure("reset_performance_metrics", "Failed to reset performance metrics", e);
			return ResponseEntity.internalServerError()
				.body(ActionResponse.error("Failed to reset metrics: " + e.getMessage()));
		}
	}

}
