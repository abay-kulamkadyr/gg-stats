package com.abe.gg_stats.dto;

public record TeamDto(Long teamId, Integer rating, Integer wins, Integer losses, Integer lastMatchTime, String name,
		String tag, String logoUrl) {

}
