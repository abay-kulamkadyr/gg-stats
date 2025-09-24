package com.abe.gg_stats.controller;

import com.abe.gg_stats.service.AggregationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/aggregations")
class AggregationsController {

	private final AggregationService aggregationService;

	@Autowired
	AggregationsController(AggregationService aggregationService) {
		this.aggregationService = aggregationService;
	}

	@PostMapping("/refresh")
	ResponseEntity<?> refresh() {
		aggregationService.refreshPatchesAndAggregations();
		return ResponseEntity.ok().build();
	}

}
