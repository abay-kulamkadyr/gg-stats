package com.abe.gg_stats.dto;

public record TeamDto(long teamId, int rating, int wins, int losses, long lastMatchTime, String name, String tag,
		String logoUrl) {

}
