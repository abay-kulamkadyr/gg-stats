package com.abe.gg_stats.controller;

import com.abe.gg_stats.service.rate_limit.RateLimitingQueryService;
import com.abe.gg_stats.service.rate_limit.OpenDotaRateLimitingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/monitoring/rate-limits")
class RateLimitController {

	private final RateLimitingQueryService rateLimitingQueryService;

	@Autowired
	RateLimitController(RateLimitingQueryService rateLimitingQueryService) {
		this.rateLimitingQueryService = rateLimitingQueryService;
	}

	@GetMapping
	ResponseEntity<OpenDotaRateLimitingService.RateLimitStatus> getRateLimitStatus() {
		return rateLimitingQueryService.getStatus();
	}

}
