package com.abe.gg_stats.batch;

import com.abe.gg_stats.repository.HeroRepository;
import com.abe.gg_stats.service.OpenDotaApiService;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.Iterator;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.NonTransientResourceException;
import org.springframework.batch.item.ParseException;
import org.springframework.batch.item.UnexpectedInputException;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class HeroRankingReader implements ItemReader<JsonNode> {

	private final HeroRepository heroRepository;

	private final OpenDotaApiService openDotaApiService;

	private Iterator<JsonNode> rankingIterator;

	private Iterator<Integer> heroIdIterator;

	private boolean initialized = false;

	private Integer currentHeroId; // Track current hero for logging

	@Override
	public JsonNode read() throws Exception, UnexpectedInputException, ParseException, NonTransientResourceException {
		if (!initialized) {
			heroIdIterator = heroRepository.findAllIds().iterator();
			initialized = true;
			log.info("Initialized hero ranking reader with {} heroes", heroRepository.findAllIds().size());
		}

		while ((rankingIterator == null || !rankingIterator.hasNext()) && heroIdIterator.hasNext()) {
			currentHeroId = heroIdIterator.next();
			log.debug("Fetching rankings for hero_id: {}", currentHeroId);

			Optional<JsonNode> response = openDotaApiService.getHeroRanking(currentHeroId);

			if (response.isPresent()) {
				JsonNode responseData = response.get();
				log.debug("API response structure for hero {}: {}", currentHeroId, responseData.toString());

				if (responseData.has("rankings") && responseData.get("rankings").isArray()) {
					JsonNode rankings = responseData.get("rankings");
					if (rankings.size() > 0) {
						// Get iterator from the rankings array, not the root response
						rankingIterator = rankings.elements();
						log.info("Loaded {} rankings for hero_id {}", rankings.size(), currentHeroId);

						// Add hero_id to each ranking since it might not be in the API
						// response
						// We'll handle this in the processor
					}
					else {
						log.warn("Empty rankings array for hero_id {}", currentHeroId);
					}
				}
				else {
					log.warn("No 'rankings' field or not an array for hero_id {}", currentHeroId);
				}
			}
			else {
				log.warn("No response from API for hero_id {}", currentHeroId);
			}
		}

		if (rankingIterator != null && rankingIterator.hasNext()) {
			JsonNode ranking = rankingIterator.next();

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

}