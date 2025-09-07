package com.abe.gg_stats.dto.response;

import com.abe.gg_stats.service.CircuitBreakerService;
import com.abe.gg_stats.service.RateLimitingService;
import org.springframework.boot.actuate.health.Health;

import java.time.Instant;
import java.util.Map;

public class SystemHealthResponse {

	private final Instant timestamp;

	private final String overallStatus;

	private final Health apiHealth;

	private final CircuitBreakerService.CircuitBreakerStatus circuitBreakerStatus;

	private final RateLimitingService.RateLimitStatus rateLimitStatus;

	private final Map<String, Object> performanceMetrics;

	private SystemHealthResponse(Builder builder) {
		this.timestamp = builder.timestamp;
		this.overallStatus = builder.overallStatus;
		this.apiHealth = builder.apiHealth;
		this.circuitBreakerStatus = builder.circuitBreakerStatus;
		this.rateLimitStatus = builder.rateLimitStatus;
		this.performanceMetrics = builder.performanceMetrics;
	}

	public static Builder builder() {
		return new Builder();
	}

	public Instant getTimestamp() {
		return timestamp;
	}

	public String getOverallStatus() {
		return overallStatus;
	}

	public Health getApiHealth() {
		return apiHealth;
	}

	public CircuitBreakerService.CircuitBreakerStatus getCircuitBreakerStatus() {
		return circuitBreakerStatus;
	}

	public RateLimitingService.RateLimitStatus getRateLimitStatus() {
		return rateLimitStatus;
	}

	public Map<String, Object> getPerformanceMetrics() {
		return performanceMetrics;
	}

	public static class Builder {

		private Instant timestamp;

		private String overallStatus;

		private Health apiHealth;

		private CircuitBreakerService.CircuitBreakerStatus circuitBreakerStatus;

		private RateLimitingService.RateLimitStatus rateLimitStatus;

		private Map<String, Object> performanceMetrics;

		public Builder timestamp(Instant timestamp) {
			this.timestamp = timestamp;
			return this;
		}

		public Builder overallStatus(String overallStatus) {
			this.overallStatus = overallStatus;
			return this;
		}

		public Builder apiHealth(Health apiHealth) {
			this.apiHealth = apiHealth;
			return this;
		}

		public Builder circuitBreakerStatus(CircuitBreakerService.CircuitBreakerStatus circuitBreakerStatus) {
			this.circuitBreakerStatus = circuitBreakerStatus;
			return this;
		}

		public Builder rateLimitStatus(RateLimitingService.RateLimitStatus rateLimitStatus) {
			this.rateLimitStatus = rateLimitStatus;
			return this;
		}

		public Builder performanceMetrics(Map<String, Object> performanceMetrics) {
			this.performanceMetrics = performanceMetrics;
			return this;
		}

		public SystemHealthResponse build() {
			return new SystemHealthResponse(this);
		}

	}

}
