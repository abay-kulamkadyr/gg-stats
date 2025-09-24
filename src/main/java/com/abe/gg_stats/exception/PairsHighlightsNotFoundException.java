package com.abe.gg_stats.exception;

public class PairsHighlightsNotFoundException extends RuntimeException {

	public PairsHighlightsNotFoundException(String view, int weekOffset, int limit) {
		super("Pair highlights not found with view: " + view + " weekOffset :" + weekOffset + " limit: " + limit);
	}

}
