package com.abe.gg_stats.exception;

import java.time.Instant;
import lombok.Getter;

/**
 * Custom exception for configuration errors
 */
@Getter
public class ConfigurationException extends RuntimeException {

	// Getters
	private final String configKey;

	private final Object configValue;

	private final String validationRule;

	private final Instant timestamp;

	public ConfigurationException(String configKey, Object configValue, String message) {
		super(String.format("Configuration error for key '%s' with value '%s': %s", configKey, configValue, message));
		this.configKey = configKey;
		this.configValue = configValue;
		this.validationRule = message;
		this.timestamp = Instant.now();
	}

}