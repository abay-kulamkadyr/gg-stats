package com.abe.gg_stats.dto.response;

public record HeroPairsDto(int heroIdA, int heroIdB, long gamesTogether, double support, double confidence, double lift,
		Double deltaSupport, Double deltaLift) {
}
