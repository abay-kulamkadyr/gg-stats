package com.abe.gg_stats.batch;

import com.abe.gg_stats.service.OpenDotaApiService;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemReader;

import java.time.LocalDateTime;
import java.time.Duration;
import java.util.Iterator;
import java.util.Optional;

@Slf4j
public abstract class BaseApiReader<T> implements ItemReader<T> {

	protected final OpenDotaApiService openDotaApiService;

	protected Iterator<T> dataIterator;

	protected boolean initialized = false;

	protected BaseApiReader(OpenDotaApiService openDotaApiService) {
		this.openDotaApiService = openDotaApiService;
	}

	@Override
	public T read() throws Exception {
		if (!initialized) {
			initialize();
			initialized = true; // Set initialized to true after the first call to
								// initialize()
		}

		if (dataIterator != null && dataIterator.hasNext()) {
			return dataIterator.next();
		}

		return null; // End of data
	}

	protected abstract void initialize();

	/**
	 * Check if data needs to be refreshed based on expiration
	 */
	protected boolean needsRefresh(LocalDateTime lastUpdate, Duration expiration) {
		if (lastUpdate == null) {
			return true;
		}

		LocalDateTime now = LocalDateTime.now();
		LocalDateTime expirationTime = lastUpdate.plus(expiration);
		return now.isAfter(expirationTime);
	}

	/**
	 * Fetch data from API and convert to iterator
	 */
	protected Optional<Iterator<T>> fetchFromApi(String endpoint, String description) {
		try {
			Optional<JsonNode> apiData = openDotaApiService.makeApiCall(endpoint);
			if (apiData.isPresent() && apiData.get().isArray()) {
				Iterator<T> iterator = convertApiResponseToIterator(apiData.get());
				log.info("Successfully fetched {} from API: {} items", description, apiData.get().size());
				return Optional.of(iterator);
			}
			else {
				log.warn("Failed to fetch {} from API or response is not an array", description);
				return Optional.empty();
			}
		}
		catch (Exception e) {
			log.error("Error fetching {} from API: {}", description, e.getMessage(), e);
			return Optional.empty();
		}
	}

	/**
	 * Convert API response to appropriate iterator type
	 */
	protected abstract Iterator<T> convertApiResponseToIterator(JsonNode apiResponse);

	/**
	 * Get the expiration period for this data type
	 */
	protected abstract Duration getExpiration();

	/**
	 * Get the API endpoint for this data type
	 */
	protected abstract String getApiEndpoint();

	/**
	 * Get the description for this data type (for logging)
	 */
	protected abstract String getDataTypeDescription();

}
