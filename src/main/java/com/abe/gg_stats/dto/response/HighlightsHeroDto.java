package com.abe.gg_stats.dto.response;

public record HighlightsHeroDto(int heroId, long matches, long picks, double pickRate, Double deltaVsPrev) {
}
