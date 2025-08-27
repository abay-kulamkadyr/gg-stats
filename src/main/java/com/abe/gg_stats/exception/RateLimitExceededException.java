package com.abe.gg_stats.exception;

import java.time.Instant;
import lombok.Getter;

/**
 * Custom exception for rate limiting scenarios
 */
@Getter
public class RateLimitExceededException extends RuntimeException {

	// Getters
	private final String endpoint;

	private final int remainingRequests;

	private final long resetTimeMs;

	private final Instant timestamp;

	public RateLimitExceededException(String endpoint, int remainingRequests, long resetTimeMs) {
		super(String.format("Rate limit exceeded for endpoint %s. Remaining: %d, Reset in: %d ms", endpoint,
				remainingRequests, resetTimeMs));
		this.endpoint = endpoint;
		this.remainingRequests = remainingRequests;
		this.resetTimeMs = resetTimeMs;
		this.timestamp = Instant.now();
	}

	public RateLimitExceededException(String endpoint, int remainingRequests, long resetTimeMs, String reason) {
		super(String.format("Rate limit exceeded for endpoint %s. Reason: %s. Remaining: %d, Reset in: %d ms", endpoint,
				reason, remainingRequests, resetTimeMs));
		this.endpoint = endpoint;
		this.remainingRequests = remainingRequests;
		this.resetTimeMs = resetTimeMs;
		this.timestamp = Instant.now();
	}

}
