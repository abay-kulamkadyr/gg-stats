package com.abe.gg_stats.dto.response;

import java.time.Instant;
import org.springframework.http.HttpStatus;

public record ApiErrorResponse(int status, String error, String message, String path, Instant timestamp) {
	public static ApiErrorResponse of(HttpStatus status, String message, String path) {
		return new ApiErrorResponse(status.value(), status.getReasonPhrase(), message, path, Instant.now());
	}
}
