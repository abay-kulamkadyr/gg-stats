package com.abe.gg_stats.dto.response;

import java.util.List;

public record HighlightsDuoDto(String bucketValue, String view, long matches, List<HighlightsHeroPairsDto> pairs) {
}
