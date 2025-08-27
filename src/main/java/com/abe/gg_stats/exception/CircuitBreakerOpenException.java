package com.abe.gg_stats.exception;

public class CircuitBreakerOpenException extends RuntimeException {

	public CircuitBreakerOpenException(String message) {
		super(message);
	}

}
