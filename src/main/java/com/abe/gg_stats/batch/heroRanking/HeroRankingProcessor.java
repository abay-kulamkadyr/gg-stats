package com.abe.gg_stats.batch.heroRanking;

import com.abe.gg_stats.batch.BaseProcessor;
import com.abe.gg_stats.entity.HeroRanking;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Component
@Slf4j
public class HeroRankingProcessor extends BaseProcessor<JsonNode, List<HeroRanking>> {

	@Override
	protected boolean isValidInput(JsonNode item) {
		// Check for required root fields
		if (!item.has("hero_id") || item.get("hero_id").isNull()) {
			log.debug("Hero ranking data missing or null 'hero_id' field in the root object.");
			return false;
		}

		if (!item.has("rankings") || !item.get("rankings").isArray()) {
			log.debug("Hero ranking data missing or 'rankings' field is not an array.");
			return false;
		}

		return true;
	}

	@Override
	protected List<HeroRanking> processItem(JsonNode item) {
		// Get the heroId from the root of the JSON
		Integer heroId = item.get("hero_id").asInt();
		JsonNode rankingsNode = item.get("rankings");

		// Process each individual ranking object within the "rankings" array
		return StreamSupport.stream(rankingsNode.spliterator(), false).map(rankingNode -> {
			try {
				// Extract individual fields for each ranking
				Long accountId = Optional.ofNullable(rankingNode.get("account_id")).map(JsonNode::asLong).orElse(null);
				Double score = Optional.ofNullable(rankingNode.get("score")).map(JsonNode::asDouble).orElse(null);

				// Validate individual ranking items
				if (accountId == null || accountId <= 0) {
					log.warn("Invalid account_id in ranking for hero {}: {}", heroId, rankingNode.get("account_id"));
					return null;
				}

				// Create and return the HeroRanking entity
				HeroRanking ranking = new HeroRanking();
				ranking.setHeroId(heroId);
				ranking.setAccountId(accountId);
				ranking.setScore(score);
				return ranking;
			}
			catch (Exception e) {
				log.error("Error processing a hero ranking item for hero {}: {}", heroId, rankingNode, e);
				return null;
			}
		})
			.filter(java.util.Objects::nonNull) // Remove any null items from the stream
			.collect(Collectors.toList());
	}

	@Override
	protected String getItemTypeDescription() {
		return "hero ranking list";
	}

}