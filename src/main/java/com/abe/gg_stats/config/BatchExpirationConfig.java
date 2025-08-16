package com.abe.gg_stats.config;

import com.abe.gg_stats.util.TimeUnitParser;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;
import java.time.Duration;

/**
 * Configuration for batch job data expiration periods. Centralizes expiration logic
 * across all batch components with flexible time unit support. Supported time units: - m:
 * minutes (e.g., "30m") - h: hours (e.g., "2h") - d: days (e.g., "1d") - w: weeks (e.g.,
 * "1w") - mo: months (e.g., "3mo") - y: years (e.g., "1y")
 */
@Configuration
@ConfigurationProperties(prefix = "app.batch.expiration")
@Data
@Slf4j
public class BatchExpirationConfig {

	private Map<String, String> durations = new HashMap<>();

	private Map<String, Duration> parsedDurations = new HashMap<>();

	@PostConstruct
	public void validateAndParseConfigurations() {
		log.info("Validating and parsing batch expiration configurations...");

		// Define default values if not provided in properties
		durations.putIfAbsent("heroes", "4mo");
		durations.putIfAbsent("teams", "6mo");
		durations.putIfAbsent("notablePlayers", "3mo");
		durations.putIfAbsent("heroRankings", "1mo");
		durations.putIfAbsent("players", "6h");
		durations.putIfAbsent("default", "3mo");

		durations.forEach((key, value) -> {
			try {
				Duration duration = TimeUnitParser.parse(value);
				parsedDurations.put(key, duration);
				log.info("{} expiration: {} (parsed to {})", key, value, formatDuration(duration));
			}
			catch (ParseException e) {
				log.error("Invalid expiration configuration for {}: '{}'", key, value);
				throw new IllegalArgumentException("Invalid expiration configuration for " + key + ": " + value, e);
			}
		});

		log.info("Batch expiration configurations validated and parsed successfully.");
	}

	private String formatDuration(Duration duration) {
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

	public Duration getHeroesDuration() {
		return parsedDurations.get("heroes");
	}

	public Duration getTeamsDuration() {
		return parsedDurations.get("teams");
	}

	public Duration getNotablePlayersDuration() {
		return parsedDurations.get("notablePlayers");
	}

	public Duration getHeroRankingsDuration() {
		return parsedDurations.get("heroRankings");
	}

}
