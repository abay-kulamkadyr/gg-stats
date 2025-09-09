package com.abe.gg_stats.controller;

import com.abe.gg_stats.repository.jdbc.HighlightsDao;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/pro/highlights")
@RequiredArgsConstructor
public class HighlightsController {

	private final HighlightsDao dao;

	@GetMapping
	public ResponseEntity<?> highlights(@RequestParam(defaultValue = "patch") String bucket,
			@RequestParam(required = false) String value, @RequestParam(defaultValue = "5") int limit,
			@RequestParam(required = false, defaultValue = "lift") String sort,
			@RequestParam(required = false, defaultValue = "0") int weekOffset) {
		if (value == null || value.isBlank()) {
			String latest = weekOffset == 0 ? dao.latestBucketValue(bucket)
					: dao.bucketValueByOffset(bucket, weekOffset);
			if (latest == null) {
				return ResponseEntity.badRequest().body("No highlights available yet for bucket: " + bucket);
			}
			value = latest;
		}
		long matches = dao.matchesForBucket(bucket, value);
		List<HighlightsDao.HeroRow> heroes = dao.topHeroes(bucket, value, limit);
		List<HighlightsDao.PairRow> pairs = dao.topPairs(bucket, value, limit, sort);
		return ResponseEntity.ok(new HighlightsResponse(matches, heroes, pairs));
	}

	public record HighlightsResponse(long matches, List<HighlightsDao.HeroRow> heroes,
			List<HighlightsDao.PairRow> pairs) {
	}

	@GetMapping("/pairs")
	public ResponseEntity<?> pairHighlights(@RequestParam(required = false, defaultValue = "synergy") String view,
			@RequestParam(required = false, defaultValue = "0") int weekOffset,
			@RequestParam(required = false, defaultValue = "10") int limit) {
		String bucket = "patch_week";
		String bucketValue = weekOffset == 0 ? dao.latestBucketValue(bucket)
				: dao.bucketValueByOffset(bucket, weekOffset);
		if (bucketValue == null) {
			return ResponseEntity.badRequest().body("No highlights available yet for bucket: " + bucket);
		}

		String sort;
		String normalized = view == null ? "synergy" : view.toLowerCase();
		sort = switch (normalized) {
			case "emerging-synergy", "emerging", "trending-synergy" -> "delta_lift";
			case "trending-popularity", "emerging-popularity", "popular-trending" -> "delta_support";
			default -> "lift";
		};

		long matches = dao.matchesForBucket(bucket, bucketValue);
		List<HighlightsDao.PairWithHeroRow> pairs = dao.topPairsWithHeroes(bucket, bucketValue, limit, sort);
		return ResponseEntity.ok(new PairHighlightsResponse(bucketValue, view, matches, pairs));
	}

	public record PairHighlightsResponse(String bucketValue, String view, long matches,
			List<HighlightsDao.PairWithHeroRow> pairs) {
	}

}
