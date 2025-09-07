package com.abe.gg_stats.service;

import com.abe.gg_stats.util.LoggingUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class RateLimitingQueryService {

	private final RateLimitingService rateLimitingService;

	public RateLimitingQueryService(RateLimitingService rateLimitingService) {
		this.rateLimitingService = rateLimitingService;
	}

	public ResponseEntity<RateLimitingService.RateLimitStatus> getStatus() {
		RateLimitingService.RateLimitStatus status = rateLimitingService.getStatus();
		LoggingUtils.logDebug("Retrieved rate limit status", () -> "availableTokens=" + status.availableTokens(),
				() -> "remainingDaily=" + status.remainingDailyRequests());
		return ResponseEntity.ok(status);
	}

}
