package com.abe.gg_stats.batch;

import com.abe.gg_stats.config.batch.BatchExpirationConfig;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Duration;
import java.time.Instant;
import java.util.Iterator;
import java.util.Optional;
import org.springframework.batch.item.ItemReader;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class BaseApiReader implements ItemReader<JsonNode> {

	protected final BatchExpirationConfig batchExpirationConfig;

	protected Iterator<JsonNode> dataIterator;

	protected boolean initialized = false;

	@Autowired
	protected BaseApiReader(BatchExpirationConfig batchExpirationConfig) {
		this.batchExpirationConfig = batchExpirationConfig;
	}

	@Override
	public JsonNode read() {
		if (!initialized) {
			initializeData();
			initialized = true;
		}

		if (dataIterator != null && dataIterator.hasNext()) {
			return dataIterator.next();
		}

		return null;
	}

	protected void initializeData() {
		Optional<Instant> latestUpdate = this.findLatestUpdate();

		if (latestUpdate.isPresent() && noRefreshNeeded(latestUpdate.get())) {
			return;
		}

		Optional<JsonNode> apiData = fetchApiData();
		apiData.ifPresent(jsonNode -> this.dataIterator = jsonNode.elements());
	}

	/**
	 * Check if data needs to be refreshed based on expiration
	 */
	protected boolean noRefreshNeeded(Instant lastUpdate) {
		if (lastUpdate == null) {
			return false;
		}
		Duration expiration = batchExpirationConfig.getDurationByConfigName(getExpirationConfigName());
		Instant now = Instant.now();
		Instant expirationTime = lastUpdate.plus(expiration);
		boolean needsRefresh = now.isAfter(expirationTime);
		return !needsRefresh;
	}

	/**
	 * Abstract method for subclasses to provide their specific API call
	 */
	protected abstract Optional<JsonNode> fetchApiData();

	/**
	 * Abstract method to get the latest update timestamp from the repository
	 */
	protected abstract Optional<Instant> findLatestUpdate();

	/**
	 * Get the description for this data type
	 */
	protected abstract String getExpirationConfigName();

}
