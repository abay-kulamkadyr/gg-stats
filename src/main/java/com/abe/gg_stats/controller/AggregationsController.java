package com.abe.gg_stats.controller;

import com.abe.gg_stats.service.AggregationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/aggregations")
@RequiredArgsConstructor
public class AggregationsController {

	private final AggregationService aggregationService;

	@PostMapping("/refresh")
	public ResponseEntity<?> refresh() {
		aggregationService.refreshPatchesAndAggregations();
		return ResponseEntity.ok().build();
	}

}
