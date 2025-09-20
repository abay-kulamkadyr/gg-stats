package com.abe.gg_stats.exception;

import lombok.Getter;

import java.time.Instant;
import java.util.Map;

@Getter
public class CircuitBreakerException extends RuntimeException {

	private final String serviceName;

	private final String state;

	private final Instant timestamp;

	private final Map<String, Object> additionalInfo;

	public CircuitBreakerException(String serviceName, String state, String message) {
		super(message);
		this.serviceName = serviceName;
		this.state = state;
		this.timestamp = Instant.now();
		this.additionalInfo = Map.of();
	}

	public CircuitBreakerException(String serviceName, String state, String message,
			Map<String, Object> additionalInfo) {
		super(message);
		this.serviceName = serviceName;
		this.state = state;
		this.timestamp = Instant.now();
		this.additionalInfo = additionalInfo != null ? Map.copyOf(additionalInfo) : Map.of();
	}

}
