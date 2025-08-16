package com.abe.gg_stats.batch.heroRanking;

import com.abe.gg_stats.batch.BaseApiReader;
import com.abe.gg_stats.config.BatchExpirationConfig;
import com.abe.gg_stats.repository.HeroRepository;
import com.abe.gg_stats.service.OpenDotaApiService;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Iterator;
import java.util.Optional;

/**
 * Reader for HeroRanking entities with improved error handling and performance. Fetches
 * hero rankings from the OpenDota API and provides them for processing.
 */
@Component
@Slf4j
public class HeroRankingReader extends BaseApiReader<JsonNode> {

	private final HeroRepository heroRepository;

	private final BatchExpirationConfig expirationConfig;

	private Iterator<Integer> heroIdIterator;

	private Integer currentHeroId; // Track current hero for logging

	public HeroRankingReader(OpenDotaApiService openDotaApiService, HeroRepository heroRepository,
			BatchExpirationConfig expirationConfig) {
		super(openDotaApiService);
		this.heroRepository = heroRepository;
		this.expirationConfig = expirationConfig;
	}

	@Override
	protected void initialize() {
		heroIdIterator = heroRepository.findAllIds().iterator();
		log.info("Initialized hero ranking reader with {} heroes", heroRepository.findAllIds().size());
	}

	@Override
	public JsonNode read() throws Exception {
		if (!initialized) {
			initialize();
		}

		// Check if we need to fetch new rankings for the next hero
		while ((dataIterator == null || !dataIterator.hasNext()) && heroIdIterator.hasNext()) {
			currentHeroId = heroIdIterator.next();
			log.debug("Fetching rankings for hero_id: {}", currentHeroId);

			Optional<Iterator<JsonNode>> rankings = fetchFromApi(getApiEndpoint() + currentHeroId,
					"rankings for hero " + currentHeroId);

			if (rankings.isPresent()) {
				this.dataIterator = rankings.get();
				log.info("Loaded rankings for hero_id {}", currentHeroId);
			}
			else {
				log.debug("No response from API for hero_id {}", currentHeroId);
			}
		}

		if (dataIterator != null && dataIterator.hasNext()) {
			JsonNode ranking = dataIterator.next();

			// The API response doesn't include hero_id in individual rankings,
			// so we need to add it from our current context
			if (ranking.isObject()) {
				com.fasterxml.jackson.databind.node.ObjectNode rankingObj = (com.fasterxml.jackson.databind.node.ObjectNode) ranking;

				// Always set the hero_id since API rankings don't include it
				rankingObj.put("hero_id", currentHeroId);

				log.debug("Added hero_id {} to ranking: {}", currentHeroId,
						ranking.has("account_id") ? ranking.get("account_id").asLong() : "unknown");
			}

			return ranking;
		}

		log.info("No more rankings to process");
		return null; // End of data
	}

	@Override
	protected Iterator<JsonNode> convertApiResponseToIterator(JsonNode apiResponse) {
		// This method is not used in this reader since we handle the conversion in read()
		// The API response structure is different for hero rankings
		return null;
	}

	@Override
	protected Duration getExpiration() {
		return expirationConfig.getHeroRankingsDuration();
	}

	@Override
	protected String getApiEndpoint() {
		return "/rankings?hero_id=";
	}

	@Override
	protected String getDataTypeDescription() {
		return "hero rankings";
	}

	/**
	 * Custom exception for hero ranking read errors
	 */
	public static class HeroRankingReadException extends RuntimeException {

		public HeroRankingReadException(String message) {
			super(message);
		}

		public HeroRankingReadException(String message, Throwable cause) {
			super(message, cause);
		}

	}

}