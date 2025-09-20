package com.abe.gg_stats.dto.response;

import java.util.List;

public record HighlightsDto(long matches, List<HighlightsHeroDto> heroes, List<HeroPairsDto> pairs) {
}
