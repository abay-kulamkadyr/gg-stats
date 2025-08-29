package com.abe.gg_stats.config;

import com.abe.gg_stats.util.LoggingConstants;
import com.abe.gg_stats.util.LoggingUtils;
import com.abe.gg_stats.util.MDCLoggingContext;
import com.abe.gg_stats.util.TimeUnitParser;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;
import lombok.Data;
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
		// Set up MDC context for configuration logging
		String correlationId = MDCLoggingContext.getOrCreateCorrelationId();
		MDCLoggingContext.updateContext("operationType", LoggingConstants.OPERATION_TYPE_SERVICE);
		MDCLoggingContext.updateContext("serviceName", "BatchExpirationConfig");
		MDCLoggingContext.updateContext("operationName", LoggingConstants.OPERATION_CONFIGURATION_VALIDATION);

		LoggingUtils.logOperationStart("Validating and parsing batch expiration configurations",
			"correlationId=" + correlationId,
			"operationType=" + LoggingConstants.OPERATION_TYPE_SERVICE,
			"serviceName=BatchExpirationConfig");

		// Use individual properties for better binding
		durations.put("heroes", heroes != null ? heroes : "4mo");
		durations.put("teams", teams != null ? teams : "6mo");
		durations.put("notableplayers", notableplayers != null ? notableplayers : "3mo");
		durations.put("herorankings", herorankings != null ? herorankings : "1mo");
		durations.put("players", players != null ? players : "6h");
		durations.put("default", defaultExpiration != null ? defaultExpiration : "3mo");

		LoggingUtils.logDebug("Using properties from configuration file",
			"correlationId=" + correlationId,
			"serviceName=BatchExpirationConfig");
		LoggingUtils.logDebug("Heroes expiration: " + (heroes != null ? heroes : "4mo (default)"),
			"correlationId=" + correlationId,
			"serviceName=BatchExpirationConfig");
		LoggingUtils.logDebug("Teams expiration: " + (teams != null ? teams : "6mo (default)"),
			"correlationId=" + correlationId,
			"serviceName=BatchExpirationConfig");
		LoggingUtils.logDebug("Notable players expiration: " + (notableplayers != null ? notableplayers : "3mo (default)"),
			"correlationId=" + correlationId,
			"serviceName=BatchExpirationConfig");
		LoggingUtils.logDebug("Hero rankings expiration: " + (herorankings != null ? herorankings : "1mo (default)"),
			"correlationId=" + correlationId,
			"serviceName=BatchExpirationConfig");
		LoggingUtils.logDebug("Players expiration: " + (players != null ? players : "6h (default)"),
			"correlationId=" + correlationId,
			"serviceName=BatchExpirationConfig");
		LoggingUtils.logDebug("Default expiration: " + (defaultExpiration != null ? defaultExpiration : "3mo (default)"),
			"correlationId=" + correlationId,
			"serviceName=BatchExpirationConfig");

		durations.forEach((key, value) -> {
			try {
				Duration duration = TimeUnitParser.parse(value);
				parsedDurations.put(key, duration);
				LoggingUtils.logDebug(key + " expiration: " + value + " (parsed to " + formatDuration(duration) + ")",
					"correlationId=" + correlationId,
					"serviceName=BatchExpirationConfig",
					"configKey=" + key,
					"configValue=" + value);
			}
			catch (ParseException e) {
				LoggingUtils.logOperationFailure("Configuration validation", "Invalid expiration configuration for " + key + ": " + value, e,
					"correlationId=" + correlationId,
					"serviceName=BatchExpirationConfig",
					"configKey=" + key,
					"configValue=" + value);
				throw new IllegalArgumentException("Invalid expiration configuration for " + key + ": " + value, e);
			}
		});

		LoggingUtils.logOperationSuccess("Batch expiration configurations validation",
			"correlationId=" + correlationId,
			"serviceName=BatchExpirationConfig",
			"totalConfigurations=" + durations.size());
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
