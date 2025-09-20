package com.abe.gg_stats.dto.response;

public record HighlightsHeroPairsDto(int heroIdA, int heroIdB, long gamesTogether, double support, double confidence,
		double lift, Double deltaSupport, Double deltaLift, String heroALocalizedName, String heroBLocalizedName,
		String heroAName, String heroBName, String heroACdnName, String heroBCdnName, String heroAImgUrl,
		String heroBImgUrl) {
}
