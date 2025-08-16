package com.abe.gg_stats.batch.heroRanking;

import com.abe.gg_stats.batch.BaseProcessor;
import com.abe.gg_stats.entity.HeroRanking;
import com.abe.gg_stats.repository.HeroRankingRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.Optional;

/**
 * Processor for HeroRanking entities with improved error handling and validation.
 * Implements proper exception handling and input validation.
 */
@Component
@Slf4j
public class HeroRankingProcessor extends BaseProcessor<JsonNode, HeroRanking> {

	private final HeroRankingRepository heroRankingRepository;

	@Autowired
	public HeroRankingProcessor(HeroRankingRepository heroRankingRepository) {
		this.heroRankingRepository = heroRankingRepository;
	}

	@Override
	protected boolean isValidInput(JsonNode item) {
		if (item == null) {
			return false;
		}

		// Check for required fields
		if (!item.has("account_id") || item.get("account_id").isNull()) {
			log.debug("Hero ranking data missing or null 'account_id' field");
			return false;
		}

		if (!item.has("hero_id") || item.get("hero_id").isNull()) {
			log.debug("Hero ranking data missing or null 'hero_id' field");
			return false;
		}

		// Validate account_id is a positive long
		try {
			long accountId = item.get("account_id").asLong();
			if (accountId <= 0) {
				log.debug("Hero ranking account_id must be positive, got: {}", accountId);
				return false;
			}
		}
		catch (Exception e) {
			log.debug("Hero ranking account_id is not a valid long: {}", item.get("account_id"));
			return false;
		}

		// Validate hero_id is a positive integer
		try {
			int heroId = item.get("hero_id").asInt();
			if (heroId <= 0) {
				log.debug("Hero ranking hero_id must be positive, got: {}", heroId);
				return false;
			}
		}
		catch (Exception e) {
			log.debug("Hero ranking hero_id is not a valid integer: {}", item.get("hero_id"));
			return false;
		}

		return true;
	}

	@Override
	protected HeroRanking processItem(JsonNode item) {
		Long accountId = item.get("account_id").asLong();
		Integer heroId = item.get("hero_id").asInt();
		Double score = item.has("score") ? item.get("score").asDouble() : null;

		Optional<HeroRanking> existingRanking = heroRankingRepository.findByHeroIdAndAccountId(accountId, heroId);

		HeroRanking ranking = existingRanking.orElseGet(HeroRanking::new);

		ranking.setAccountId(accountId);
		ranking.setHeroId(heroId);
		ranking.setScore(score);

		log.debug("Processed hero ranking: hero_id={}, account_id={}, score={}", heroId, accountId, score);
		return ranking;
	}

	@Override
	protected String getItemTypeDescription() {
		return "hero ranking";
	}

	/**
	 * Custom exception for hero ranking processing errors
	 */
	public static class HeroRankingProcessingException extends Exception {

		public HeroRankingProcessingException(String message) {
			super(message);
		}

		public HeroRankingProcessingException(String message, Throwable cause) {
			super(message, cause);
		}

	}

}
