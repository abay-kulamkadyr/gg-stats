package com.abe.gg_stats.dto.response;

import java.util.List;

public record PageResponseDto<T>(List<T> content, int page, int size, int totalPages, long totalElements) {
}
