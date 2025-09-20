package com.abe.gg_stats.service.rate_limit;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class RateLimitingQueryService {

	private final OpenDotaRateLimitingService openDotaRateLimitingService;

	public RateLimitingQueryService(OpenDotaRateLimitingService openDotaRateLimitingService) {
		this.openDotaRateLimitingService = openDotaRateLimitingService;
	}

	public ResponseEntity<OpenDotaRateLimitingService.RateLimitStatus> getStatus() {
		OpenDotaRateLimitingService.RateLimitStatus status = openDotaRateLimitingService.getStatus();
		return ResponseEntity.ok(status);
	}

}
