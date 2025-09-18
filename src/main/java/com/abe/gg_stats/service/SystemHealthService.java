package com.abe.gg_stats.service;

import com.abe.gg_stats.dto.response.SystemHealthResponse;
import com.abe.gg_stats.util.LoggingUtils;
import io.micrometer.core.annotation.Timed;
import java.time.Instant;
import org.springframework.boot.actuate.health.Health;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class SystemHealthService {

	private final OpenDotaApiService openDotaApiService;

	private final MetricsService metricsService;

	private final ServiceLogger serviceLogger;

	public SystemHealthService(OpenDotaApiService openDotaApiService, MetricsService metricsService,
			ServiceLogger serviceLogger) {
		this.openDotaApiService = openDotaApiService;
		this.metricsService = metricsService;
		this.serviceLogger = serviceLogger;
	}

	@Timed(description = "System health check")
	public ResponseEntity<SystemHealthResponse> getSystemHealth() {
		try (LoggingUtils.AutoCloseableStopWatch ignored = LoggingUtils.createStopWatch("system_health_check")) {
			Health apiHealth = openDotaApiService.health();
			// OpenDotaApiService.ApiServiceStatistics stats =
			// openDotaApiService.getStatistics();

			SystemHealthResponse response = SystemHealthResponse.builder()
				.timestamp(Instant.now())
				.overallStatus(determineOverallStatus(apiHealth))
				.apiHealth(apiHealth)
				// .circuitBreakerStatus(stats.circuitBreakerStatus())
				// .rateLimitStatus(stats.rateLimitStatus())
				.performanceMetrics(metricsService.getPerformanceMetrics())
				.build();

			serviceLogger.logServiceSuccess("SystemHealthService", "System health check completed",
					"overallStatus=" + response.getOverallStatus());

			return ResponseEntity.ok(response);
		}
	}

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

}
