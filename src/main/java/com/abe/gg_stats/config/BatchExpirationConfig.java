package com.abe.gg_stats.config;

import com.abe.gg_stats.util.TimeUnitParser;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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
@Data
@Slf4j
public class BatchExpirationConfig {

	private Map<String, String> durations = new HashMap<>();

	private Map<String, Duration> parsedDurations = new HashMap<>();

	// Individual properties for better binding
	@Value("${app.batch.expiration.heroes:4mo}")
	private String heroes;

	@Value("${app.batch.expiration.teams:6mo}")
	private String teams;

	@Value("${app.batch.expiration.notableplayers:3mo}")
	private String notableplayers;

	@Value("${app.batch.expiration.herorankings:1mo}")
	private String herorankings;

	@Value("${app.batch.expiration.players:6h}")
	private String players;

	@Value("${app.batch.expiration.default:3mo}")
	private String defaultExpiration;

	@PostConstruct
	public void validateAndParseConfigurations() {
		log.info("Validating and parsing batch expiration configurations...");

		// Use individual properties for better binding
		durations.put("heroes", heroes != null ? heroes : "4mo");
		durations.put("teams", teams != null ? teams : "6mo");
		durations.put("notableplayers", notableplayers != null ? notableplayers : "3mo");
		durations.put("herorankings", herorankings != null ? herorankings : "1mo");
		durations.put("players", players != null ? players : "6h");
		durations.put("default", defaultExpiration != null ? defaultExpiration : "3mo");

		log.info("Using properties from configuration file");
		log.info("Heroes expiration: {}", heroes != null ? heroes : "4mo (default)");
		log.info("Teams expiration: {}", teams != null ? teams : "6mo (default)");
		log.info("Notable players expiration: {}", notableplayers != null ? notableplayers : "3mo (default)");
		log.info("Hero rankings expiration: {}", herorankings != null ? herorankings : "1mo (default)");
		log.info("Players expiration: {}", players != null ? players : "6h (default)");
		log.info("Default expiration: {}", defaultExpiration != null ? defaultExpiration : "3mo (default)");

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

	public Duration getDurationByConfigName(String configName) {
		return parsedDurations.get(configName);
	}

	public Duration getHeroesDuration() {
		return parsedDurations.get("heroes");
	}

	public Duration getTeamsDuration() {
		return parsedDurations.get("teams");
	}

	public Duration getNotablePlayersDuration() {
		return parsedDurations.get("notableplayers");
	}

	public Duration getHeroRankingsDuration() {
		return parsedDurations.get("herorankings");
	}

}
