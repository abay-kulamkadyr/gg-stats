package com.abe.gg_stats.exception;

import lombok.Getter;

import java.time.Instant;

@Getter
public class ApiServiceException extends RuntimeException {

	private final String serviceName;

	private final String endpoint;

	private final int statusCode;

	private final String errorType;

	private final Instant timestamp;

	public ApiServiceException(String serviceName, String endpoint, String message) {
		super(message);
		this.serviceName = serviceName;
		this.endpoint = endpoint;
		this.statusCode = -1;
		this.errorType = "UNKNOWN";
		this.timestamp = Instant.now();
	}

	public ApiServiceException(String serviceName, String endpoint, String message, int statusCode, String errorType) {
		super(message);
		this.serviceName = serviceName;
		this.endpoint = endpoint;
		this.statusCode = statusCode;
		this.errorType = errorType;
		this.timestamp = Instant.now();
	}

	public ApiServiceException(String serviceName, String endpoint, String message, Throwable cause) {
		super(message, cause);
		this.serviceName = serviceName;
		this.endpoint = endpoint;
		this.statusCode = -1;
		this.errorType = "UNKNOWN";
		this.timestamp = Instant.now();
	}

}
