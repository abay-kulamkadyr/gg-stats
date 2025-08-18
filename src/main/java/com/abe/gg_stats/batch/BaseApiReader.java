package com.abe.gg_stats.batch;

import com.abe.gg_stats.config.BatchExpirationConfig;
import com.abe.gg_stats.service.OpenDotaApiService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemReader;

import java.time.LocalDateTime;
import java.time.Duration;
import java.util.Iterator;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
public abstract class BaseApiReader<JsonNode> implements ItemReader<JsonNode> {

	protected final OpenDotaApiService openDotaApiService;

	protected final BatchExpirationConfig batchExpirationConfig;

	protected Iterator<JsonNode> dataIterator;

	protected boolean initialized = false;

	@Autowired
	protected BaseApiReader(OpenDotaApiService openDotaApiService, BatchExpirationConfig batchExpirationConfig) {
		this.openDotaApiService = openDotaApiService;
		this.batchExpirationConfig = batchExpirationConfig;
	}

	@Override
	public JsonNode read() {
		if (!initialized) {
			initialize();
			initialized = true;
		}

		if (dataIterator != null && dataIterator.hasNext()) {
			return dataIterator.next();
		}

		return null;
	}

	protected abstract void initialize();

	/**
	 * Check if data needs to be refreshed based on expiration
	 */
	protected boolean noRefreshNeeded(LocalDateTime lastUpdate) {
		if (lastUpdate == null) {
			return false;
		}
		Duration expiration = batchExpirationConfig.getDurationByConfigName(getExpirationConfigName());
		LocalDateTime now = LocalDateTime.now();
		LocalDateTime expirationTime = lastUpdate.plus(expiration);
		return !now.isAfter(expirationTime);
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
	protected String formatDuration(Duration duration) {
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
