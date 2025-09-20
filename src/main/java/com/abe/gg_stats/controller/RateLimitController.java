package com.abe.gg_stats.controller;

import com.abe.gg_stats.service.rate_limit.RateLimitingQueryService;
import com.abe.gg_stats.service.rate_limit.OpenDotaRateLimitingService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@Controller
@ResponseBody
@RequestMapping("/api/monitoring/rate-limits")
class RateLimitController {

	private final RateLimitingQueryService rateLimitingQueryService;

	public RateLimitController(RateLimitingQueryService rateLimitingQueryService) {
		this.rateLimitingQueryService = rateLimitingQueryService;
	}

	@GetMapping
	public ResponseEntity<OpenDotaRateLimitingService.RateLimitStatus> getRateLimitStatus() {
		return rateLimitingQueryService.getStatus();
	}

}
