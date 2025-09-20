package com.abe.gg_stats.config.batch;

import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(BatchExpirationProperties.class)
@AllArgsConstructor
@Slf4j
public class BatchExpirationConfig {

	private final Map<String, Duration> expirationMap = new HashMap<>();

	private final BatchExpirationProperties properties;

	@PostConstruct
	public void initialize() {
		// Add all properties to the map and log their values
		expirationMap.put("heroes", properties.heroes());
		log.info("Heroes expiration: {}", formatDuration(properties.heroes()));

		expirationMap.put("teams", properties.teams());
		log.info("Teams expiration: {}", formatDuration(properties.teams()));

		expirationMap.put("notableplayers", properties.notableplayers());
		log.info("Notable players expiration: {}", formatDuration(properties.notableplayers()));

		expirationMap.put("herorankings", properties.herorankings());
		log.info("Hero rankings expiration: {}", formatDuration(properties.herorankings()));

		expirationMap.put("players", properties.players());
		log.info("Players expiration: {}", formatDuration(properties.players()));

		expirationMap.put("defaults", properties.defaults() == null ? Duration.ofDays(30 * 3) : properties.defaults());
		log.info("Default expiration: {}", formatDuration(properties.defaults()));
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