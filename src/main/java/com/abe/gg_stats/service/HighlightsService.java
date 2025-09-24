package com.abe.gg_stats.service;

import com.abe.gg_stats.dto.response.HeroPairsDto;
import com.abe.gg_stats.dto.response.HighlightsDto;
import com.abe.gg_stats.dto.response.HighlightsDuoDto;
import com.abe.gg_stats.dto.response.HighlightsHeroDto;
import com.abe.gg_stats.dto.response.HighlightsHeroPairsDto;
import com.abe.gg_stats.exception.HighlightsNotFoundException;
import com.abe.gg_stats.exception.PairsHighlightsNotFoundException;
import com.abe.gg_stats.repository.jdbc.HighlightsDao;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class HighlightsService {

	private final HighlightsDao dao;

	@Autowired
	public HighlightsService(HighlightsDao dao) {
		this.dao = dao;
	}

	public HighlightsDto getHighlights(String bucket, String value, int limit, String sort, int weekOffset) {
		String finalValue = value;
		if (finalValue == null || finalValue.isBlank()) {
			finalValue = weekOffset == 0 ? dao.latestBucketValue(bucket) : dao.bucketValueByOffset(bucket, weekOffset);
			if (finalValue == null) {
				throw new HighlightsNotFoundException(bucket, value, limit, sort, weekOffset);
			}
		}
		long matches = dao.matchesForBucket(bucket, finalValue);
		List<HighlightsHeroDto> heroes = dao.topHeroes(bucket, finalValue, limit);
		List<HeroPairsDto> pairs = dao.topPairs(bucket, finalValue, limit, sort);
		if (pairs == null || heroes == null) {
			throw new HighlightsNotFoundException(bucket, value, limit, sort, weekOffset);
		}
		return new HighlightsDto(matches, heroes, pairs);
	}

	public HighlightsDuoDto getPairHighlights(String view, int weekOffset, int limit) {
		String bucket = "patch_week";
		String bucketValue = weekOffset == 0 ? dao.latestBucketValue(bucket)
				: dao.bucketValueByOffset(bucket, weekOffset);
		if (bucketValue == null) {
			throw new PairsHighlightsNotFoundException(view, weekOffset, limit);
		}

		String sort;
		String normalized = view == null ? "synergy" : view.toLowerCase();
		sort = switch (normalized) {
			case "emerging-synergy", "emerging", "trending-synergy" -> "delta_lift";
			case "trending-popularity", "emerging-popularity", "popular-trending" -> "delta_support";
			default -> "lift";
		};

		long matches = dao.matchesForBucket(bucket, bucketValue);
		List<HighlightsHeroPairsDto> pairs = dao.topPairsWithHeroes(bucket, bucketValue, limit, sort);
		if (pairs == null) {
			throw new PairsHighlightsNotFoundException(view, weekOffset, limit);
		}
		return new HighlightsDuoDto(bucketValue, view, matches, pairs);
	}

}