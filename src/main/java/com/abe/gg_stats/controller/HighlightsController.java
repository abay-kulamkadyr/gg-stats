package com.abe.gg_stats.controller;

import com.abe.gg_stats.dto.response.HighlightsDto;
import com.abe.gg_stats.dto.response.HighlightsDuoDto;
import com.abe.gg_stats.service.HighlightsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@ResponseBody
@RequestMapping("/highlights")
public class HighlightsController {

	private final HighlightsService highlightsService;

	@Autowired
	public HighlightsController(HighlightsService highlightsService) {
		this.highlightsService = highlightsService;
	}

	@GetMapping
	public ResponseEntity<?> highlights(@RequestParam(defaultValue = "patch") String bucket,
			@RequestParam(required = false) String value, @RequestParam(defaultValue = "5") int limit,
			@RequestParam(required = false, defaultValue = "lift") String sort,
			@RequestParam(required = false, defaultValue = "0") int weekOffset) {

		HighlightsDto highlights = highlightsService.getHighlights(bucket, value, limit, sort, weekOffset);
		if (highlights == null) {
			return ResponseEntity.badRequest().body("No highlights available yet for bucket: " + bucket);
		}
		return ResponseEntity.ok(highlights);
	}

	@GetMapping("/pairs")
	public ResponseEntity<?> pairHighlights(@RequestParam(required = false, defaultValue = "synergy") String view,
			@RequestParam(required = false, defaultValue = "0") int weekOffset,
			@RequestParam(required = false, defaultValue = "10") int limit) {

		HighlightsDuoDto pairHighlights = highlightsService.getPairHighlights(view, weekOffset, limit);
		if (pairHighlights == null) {
			return ResponseEntity.badRequest().body("No highlights available yet for bucket: patch_week");
		}
		return ResponseEntity.ok(pairHighlights);
	}

}