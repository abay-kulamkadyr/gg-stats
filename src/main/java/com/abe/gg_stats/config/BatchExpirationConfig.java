package com.abe.gg_stats.config;

import com.abe.gg_stats.util.LoggingUtils;
import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import lombok.AllArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class that maps the BatchExpirationProperties to a Map for easy lookup
 * and handles the logging of the loaded configuration.
 */
@Configuration
@EnableConfigurationProperties(BatchExpirationProperties.class)
@AllArgsConstructor
public class BatchExpirationConfig {

	private final Map<String, Duration> expirationMap = new HashMap<>();

	private final BatchExpirationProperties properties;

	/**
	 * Initializes the expiration map and logs the loaded durations.
	 */
	@PostConstruct
	public void initialize() {
		LoggingUtils.logOperationStart("Initializing BatchExpirationConfig with properties");

		// Add all properties to the map and log their values
		expirationMap.put("heroes", properties.heroes());
		LoggingUtils.logDebug("Heroes expiration: " + formatDuration(properties.heroes()));

		expirationMap.put("teams", properties.teams());
		LoggingUtils.logDebug("Teams expiration: " + formatDuration(properties.teams()));

		expirationMap.put("notableplayers", properties.notableplayers());
		LoggingUtils.logDebug("Notable players expiration: " + formatDuration(properties.notableplayers()));

		expirationMap.put("herorankings", properties.herorankings());
		LoggingUtils.logDebug("Hero rankings expiration: " + formatDuration(properties.herorankings()));

		expirationMap.put("players", properties.players());
		LoggingUtils.logDebug("Players expiration: " + formatDuration(properties.players()));

		expirationMap.put("defaults", properties.defaults() == null ? Duration.ofDays(30 * 3) : properties.defaults());
		LoggingUtils.logDebug("Default expiration: " + formatDuration(properties.defaults()));

		LoggingUtils.logOperationSuccess("BatchExpirationConfig initialized successfully");
	}

	/**
	 * Retrieves the expiration duration by the given configuration name. If a specific
	 * duration is not found, it falls back to the default duration.
	 * @param configName The name of the configuration.
	 * @return The corresponding Duration.
	 */
	public Duration getDurationByConfigName(String configName) {
		return expirationMap.getOrDefault(configName, expirationMap.get("defaults"));
	}

	/**
	 * Formats a Duration object into a human-readable string.
	 * @param duration The Duration to format.
	 * @return A formatted string representation of the duration.
	 */
	protected String formatDuration(Duration duration) {
		if (duration == null) {
			return "N/A";
		}
		long days = duration.toDays();
		long hours = duration.toHoursPart();
		long minutes = duration.toMinutesPart();

		if (days > 0) {
			return String.format("%d days, %d hours, %d minutes", days, hours, minutes);
		}
		else if (hours > 0) {
			return String.format("%d hours, %d minutes", hours, minutes);
		}
		else {
			return String.format("%d minutes", minutes);
		}
	}

}