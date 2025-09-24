package com.abe.gg_stats.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class HighlightsNotFoundException extends RuntimeException {

	public HighlightsNotFoundException(String bucket, String value, int limit, String sort, int weekOffset) {
		super("Highlights not found with bucket : " + bucket + " value: " + value + " limit: " + limit + " sort: "
				+ sort + " weekOffset: " + weekOffset);
	}

}
