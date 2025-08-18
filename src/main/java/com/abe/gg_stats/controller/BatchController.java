package com.abe.gg_stats.controller;

import com.abe.gg_stats.service.BatchSchedulerService;
import com.abe.gg_stats.service.OpenDotaApiService;
import com.abe.gg_stats.util.LoggingUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;

import java.util.HashMap;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for managing batch jobs and system status. Provides endpoints for
 * triggering jobs manually and monitoring system health.
 */
@RestController
@RequestMapping("/api/batch")
@RequiredArgsConstructor
@Slf4j
public class BatchController {

	private final BatchSchedulerService batchSchedulerService;

	private final OpenDotaApiService openDotaApiService;

	/**
	 * Get overall system status including API rate limits and circuit breaker status
	 */
	@GetMapping("/status")
	public ResponseEntity<Map<String, Object>> getSystemStatus() {
		Map<String, Object> status = new HashMap<>();

		try {
			// Get batch scheduler status
			String schedulerStatus = batchSchedulerService.getSchedulerStatus();
			status.put("scheduler", schedulerStatus);

			// Get API rate limit status
			int remainingDailyRequests = openDotaApiService.getRemainingDailyRequests();
			status.put("apiRateLimit",
					Map.of("remainingDailyRequests", remainingDailyRequests, "totalDailyLimit", 1800));

			// Get circuit breaker status
			String circuitBreakerStatus = openDotaApiService.getCircuitBreakerStatus();
			status.put("circuitBreaker", circuitBreakerStatus);

			status.put("status", "healthy");
			LoggingUtils.logDebug("System status retrieved successfully", "status=healthy");
			return ResponseEntity.ok(status);

		}
		catch (Exception e) {
			LoggingUtils.logOperationFailure("system status retrieval", "Failed to retrieve system status", e);
			status.put("status", "error");
			status.put("message", "Failed to retrieve system status: " + e.getMessage());
			return ResponseEntity.internalServerError().body(status);
		}
	}

	/**
	 * Trigger heroes update job manually
	 */
	@PostMapping("/jobs/heroes")
	public ResponseEntity<Map<String, Object>> triggerHeroesUpdate() {
		Map<String, Object> response = new HashMap<>();

		try {
			boolean success = batchSchedulerService.triggerHeroesUpdate();

			if (success) {
				response.put("status", "success");
				response.put("message", "Heroes update job triggered successfully");
				return ResponseEntity.ok(response);
			}
			else {
				response.put("status", "failed");
				response.put("message",
						"Failed to trigger heroes update job - insufficient API requests or other constraints");
				return ResponseEntity.badRequest().body(response);
			}

		}
		catch (Exception e) {
			log.error("Error triggering heroes update job", e);
			response.put("status", "error");
			response.put("message", "Failed to trigger heroes update job: " + e.getMessage());
			return ResponseEntity.internalServerError().body(response);
		}
	}

	/**
	 * Trigger players update job manually
	 */
	@PostMapping("/jobs/players")
	public ResponseEntity<Map<String, Object>> triggerPlayersUpdate() {
		Map<String, Object> response = new HashMap<>();

		try {
			boolean success = batchSchedulerService.triggerPlayerUpdate();

			if (success) {
				response.put("status", "success");
				response.put("message", "Players update job triggered successfully");
				return ResponseEntity.ok(response);
			}
			else {
				response.put("status", "failed");
				response.put("message",
						"Failed to trigger players update job - insufficient API requests or other constraints");
				return ResponseEntity.badRequest().body(response);
			}

		}
		catch (Exception e) {
			log.error("Error triggering players update job", e);
			response.put("status", "error");
			response.put("message", "Failed to trigger players update job: " + e.getMessage());
			return ResponseEntity.internalServerError().body(response);
		}
	}

	/**
	 * Trigger teams update job manually
	 */
	@PostMapping("/jobs/teams")
	public ResponseEntity<Map<String, Object>> triggerTeamsUpdate() {
		Map<String, Object> response = new HashMap<>();

		try {
			boolean success = batchSchedulerService.triggerTeamsUpdate();

			if (success) {
				response.put("status", "success");
				response.put("message", "Teams update job triggered successfully");
				return ResponseEntity.ok(response);
			}
			else {
				response.put("status", "failed");
				response.put("message",
						"Failed to trigger teams update job - insufficient API requests or other constraints");
				return ResponseEntity.badRequest().body(response);
			}

		}
		catch (Exception e) {
			log.error("Error triggering teams update job", e);
			response.put("status", "error");
			response.put("message", "Failed to trigger teams update job: " + e.getMessage());
			return ResponseEntity.internalServerError().body(response);
		}
	}

	/**
	 * Trigger notable players update job manually
	 */
	@PostMapping("/jobs/notable-players")
	public ResponseEntity<Map<String, Object>> triggerNotablePlayersUpdate() {
		Map<String, Object> response = new HashMap<>();

		try {
			boolean success = batchSchedulerService.triggerNotablePlayerUpdate();

			if (success) {
				response.put("status", "success");
				response.put("message", "Notable players update job triggered successfully");
				return ResponseEntity.ok(response);
			}
			else {
				response.put("status", "failed");
				response.put("message",
						"Failed to trigger notable players update job - insufficient API requests or other constraints");
				return ResponseEntity.badRequest().body(response);
			}

		}
		catch (Exception e) {
			log.error("Error triggering notable players update job", e);
			response.put("status", "error");
			response.put("message", "Failed to trigger notable players update job: " + e.getMessage());
			return ResponseEntity.internalServerError().body(response);
		}
	}

	/**
	 * Trigger hero rankings update job manually
	 */
	@PostMapping("/jobs/hero-rankings")
	public ResponseEntity<Map<String, Object>> triggerHeroRankingsUpdate() {
		Map<String, Object> response = new HashMap<>();

		try {
			boolean success = batchSchedulerService.triggerHeroRankingUpdate();

			if (success) {
				response.put("status", "success");
				response.put("message", "Hero rankings update job triggered successfully");
				return ResponseEntity.ok(response);
			}
			else {
				response.put("status", "failed");
				response.put("message",
						"Failed to trigger hero rankings update job - insufficient API requests or other constraints");
				return ResponseEntity.badRequest().body(response);
			}

		}
		catch (Exception e) {
			log.error("Error triggering hero rankings update job", e);
			response.put("status", "error");
			response.put("message", "Failed to trigger hero rankings update job: " + e.getMessage());
			return ResponseEntity.internalServerError().body(response);
		}
	}

	/**
	 * Get detailed API rate limit information for a specific endpoint
	 */
	@GetMapping("/api-rate-limit/{endpoint}")
	public ResponseEntity<Map<String, Object>> getApiRateLimitStatus(@PathVariable String endpoint) {
		Map<String, Object> response = new HashMap<>();

		try {
			var rateLimitStatus = openDotaApiService.getRateLimitStatus(endpoint);
			int remainingMinuteRequests = openDotaApiService.getRemainingMinuteRequests(endpoint);

			response.put("endpoint", endpoint);
			response.put("remainingMinuteRequests", remainingMinuteRequests);
			response.put("rateLimitStatus", rateLimitStatus.orElse(null));

			return ResponseEntity.ok(response);

		}
		catch (Exception e) {
			log.error("Error getting API rate limit status for endpoint: {}", endpoint, e);
			response.put("status", "error");
			response.put("message", "Failed to retrieve rate limit status: " + e.getMessage());
			return ResponseEntity.internalServerError().body(response);
		}
	}

}
