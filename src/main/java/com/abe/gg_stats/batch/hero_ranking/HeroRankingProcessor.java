package com.abe.gg_stats.batch.hero_ranking;

import com.abe.gg_stats.batch.BaseProcessor;
import com.abe.gg_stats.entity.HeroRanking;
import com.abe.gg_stats.util.LoggingConstants;
import com.abe.gg_stats.util.LoggingUtils;
import com.abe.gg_stats.util.MDCLoggingContext;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.springframework.stereotype.Component;

@Component
public class HeroRankingProcessor extends BaseProcessor<List<HeroRanking>> {

	@Override
	protected boolean isValidInput(JsonNode item) {
		// Set up validation context
		String correlationId = MDCLoggingContext.getOrCreateCorrelationId();
		MDCLoggingContext.updateContext("operationType", LoggingConstants.OPERATION_TYPE_BATCH);
		MDCLoggingContext.updateContext("batchType", "herorankings");

		// Check for required root fields
		final String LOG_PARSING_ERROR = "HeroRanking Json Validation Error";
		if (!item.has("hero_id") || item.get("hero_id").isNull()) {
			LoggingUtils.logWarning(LOG_PARSING_ERROR, "correlationId=" + correlationId,
					"Hero ranking data missing or null 'hero_id' field in the root object.");
			return false;
		}

		if (!item.has("rankings") || !item.get("rankings").isArray()) {
			LoggingUtils.logWarning(LOG_PARSING_ERROR, "correlationId=" + correlationId,
					"Hero ranking data missing or 'rankings' field is not an array.");
			return false;
		}

		return true;
	}

	@Override
	protected List<HeroRanking> processItem(JsonNode item) {
		// Set up processing context
		String correlationId = MDCLoggingContext.getOrCreateCorrelationId();
		MDCLoggingContext.updateContext("operationType", LoggingConstants.OPERATION_TYPE_BATCH);
		MDCLoggingContext.updateContext("batchType", "herorankings");

		// Get the heroId from the root of the JSON
		Integer heroId = item.get("hero_id").asInt();
		JsonNode rankingsNode = item.get("rankings");

		// Process each individual ranking object within the "rankings" array
		return StreamSupport.stream(rankingsNode.spliterator(), false).map(rankingNode -> {
			try {
				// Extract individual fields for each ranking
				Long accountId = Optional.ofNullable(rankingNode.get("account_id")).map(JsonNode::asLong).orElse(null);
				Double score = Optional.ofNullable(rankingNode.get("score")).map(JsonNode::asDouble).orElse(null);

				// Create and return the HeroRanking entity
				HeroRanking ranking = new HeroRanking();
				ranking.setHeroId(heroId);
				ranking.setAccountId(accountId);
				ranking.setScore(score);
				return ranking;
			}
			catch (Exception e) {
				LoggingUtils.logOperationFailure("HeroRankingProcessor",
						"Error processing a hero ranking item for hero " + heroId, e, "correlationId=" + correlationId,
						"heroId=" + heroId);
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