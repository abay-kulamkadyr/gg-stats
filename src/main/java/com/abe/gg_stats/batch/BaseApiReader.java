package com.abe.gg_stats.batch;

import com.abe.gg_stats.config.BatchExpirationConfig;
import com.abe.gg_stats.service.OpenDotaApiService;
import com.abe.gg_stats.util.LoggingUtils;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Duration;
import java.time.Instant;
import java.util.Iterator;
import org.springframework.batch.item.ItemReader;

public abstract class BaseApiReader implements ItemReader<JsonNode> {

	protected final OpenDotaApiService openDotaApiService;

	protected final BatchExpirationConfig batchExpirationConfig;

	protected Iterator<JsonNode> dataIterator;

	protected boolean initialized = false;

	protected BaseApiReader(OpenDotaApiService openDotaApiService, BatchExpirationConfig batchExpirationConfig) {
		this.openDotaApiService = openDotaApiService;
		this.batchExpirationConfig = batchExpirationConfig;
	}

	@Override
	public JsonNode read() {
		if (!initialized) {
			LoggingUtils.logOperationStart("Initializing " + getExpirationConfigName() + " reader");
			initialize();
			initialized = true;
			LoggingUtils.logOperationSuccess("Initialized " + getExpirationConfigName() + " reader");
		}

		if (dataIterator != null && dataIterator.hasNext()) {
			JsonNode item = dataIterator.next();
			LoggingUtils.logDebug("Reading item from " + getExpirationConfigName() + " batch",
					"itemType=" + getExpirationConfigName(), "item=" + (item != null ? item.toString() : "null"));
			return item;
		}

		return null;
	}

	protected abstract void initialize();

	/**
	 * Check if data needs to be refreshed based on expiration
	 */
	protected boolean noRefreshNeeded(Instant lastUpdate) {
		final String expirationConfigNameLabel = "dataType=";

		if (lastUpdate == null) {
			LoggingUtils.logDebug("No refresh needed - no previous update timestamp",
					expirationConfigNameLabel + getExpirationConfigName());
			return false;
		}

		Duration expiration = batchExpirationConfig.getDurationByConfigName(getExpirationConfigName());
		Instant now = Instant.now();
		Instant expirationTime = lastUpdate.plus(expiration);
		boolean needsRefresh = now.isAfter(expirationTime);
		if (needsRefresh) {
			LoggingUtils.logDebug("Data refresh needed", expirationConfigNameLabel + getExpirationConfigName(),
					"lastUpdate=" + lastUpdate, "expiresAt=" + expirationTime, "currentTime=" + now);
		}
		else {
			LoggingUtils.logDebug("Data refresh not needed", expirationConfigNameLabel + getExpirationConfigName(),
					"lastUpdate=" + lastUpdate, "expiresAt=" + expirationTime, "currentTime=" + now);
		}

		return !needsRefresh;
	}

	/**
	 * Get the expiration period for this data type
	 */
	protected Duration getExpiration() {
		return batchExpirationConfig.getDurationByConfigName(getExpirationConfigName());
	}

	/**
	 * Get the description for this data type
	 */
	protected abstract String getExpirationConfigName();

	/**
	 * Format duration for logging
	 */
	protected String getFormattedDurationUntilExpiration(Duration duration) {
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
