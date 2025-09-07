package com.abe.gg_stats.controller;

import com.abe.gg_stats.service.RateLimitingQueryService;
import com.abe.gg_stats.service.RateLimitingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/monitoring/rate-limits")
public class RateLimitController {

	private final RateLimitingQueryService rateLimitingQueryService;

	public RateLimitController(RateLimitingQueryService rateLimitingQueryService) {
		this.rateLimitingQueryService = rateLimitingQueryService;
	}

	@GetMapping
	public ResponseEntity<RateLimitingService.RateLimitStatus> getRateLimitStatus() {
		return rateLimitingQueryService.getStatus();
	}

}
