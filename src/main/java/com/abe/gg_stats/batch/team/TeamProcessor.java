package com.abe.gg_stats.batch.team;

import com.abe.gg_stats.batch.BaseProcessor;
import com.abe.gg_stats.entity.Team;
import com.abe.gg_stats.util.LoggingConstants;
import com.abe.gg_stats.util.LoggingUtils;
import com.abe.gg_stats.util.MDCLoggingContext;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

/**
 * Processor for Team entities with improved error handling and validation. Implements
 * proper exception handling and input validation.
 */
@Component
public class TeamProcessor extends BaseProcessor<Team> {

	@Override
	protected boolean isValidInput(JsonNode item) {
		// Set up validation context
		String correlationId = MDCLoggingContext.getOrCreateCorrelationId();
		MDCLoggingContext.updateContext("operationType", LoggingConstants.OPERATION_TYPE_BATCH);
		MDCLoggingContext.updateContext("batchType", "teams");

		if (item == null) {
			return false;
		}

		// Check for required fields
		if (!item.has("team_id") || item.get("team_id").isNull()) {
			LoggingUtils.logDebug("Team data missing or null 'team_id' field", "correlationId=" + correlationId);
			return false;
		}

		// Validate team_id is a positive long
		try {
			long teamId = item.get("team_id").asLong();
			if (teamId <= 0) {
				LoggingUtils.logDebug("Team team_id must be positive, got: {}", "correlationId=" + correlationId,
						"teamId=" + teamId);
				return false;
			}
		}
		catch (Exception e) {
			LoggingUtils.logDebug("Team team_id is not a valid long: {}", "correlationId=" + correlationId,
					"teamId=" + item.get("team_id"));
			return false;
		}

		return true;
	}

	@Override
	protected Team processItem(JsonNode item) {
		// Set up processing context
		String correlationId = MDCLoggingContext.getOrCreateCorrelationId();
		MDCLoggingContext.updateContext("operationType", LoggingConstants.OPERATION_TYPE_BATCH);
		MDCLoggingContext.updateContext("batchType", "teams");

		Team team = new Team();
		team.setTeamId(item.get("team_id").asLong());

		JsonNode ratingNode = item.get("rating");
		LoggingUtils.logDebug("Processing rating field", "correlationId=" + correlationId,
				"hasRating=" + item.has("rating"), "ratingNode=" + ratingNode,
				"isNull=" + (ratingNode != null ? ratingNode.isNull() : "N/A"),
				"isNumber=" + (ratingNode != null ? ratingNode.isNumber() : "N/A"));

		if (item.has("rating") && ratingNode != null && !ratingNode.isNull() && ratingNode.isNumber()) {
			team.setRating(ratingNode.asInt());
		}
		else {
			team.setRating(null);
		}

		team.setWins(item.has("wins") && !item.get("wins").isNull() ? item.get("wins").asInt() : 0);
		team.setLosses(item.has("losses") && !item.get("losses").isNull() ? item.get("losses").asInt() : 0);
		team.setLastMatchTime(item.has("last_match_time") ? item.get("last_match_time").asLong() : null);
		team.setName(item.has("name") ? item.get("name").asText() : null);
		team.setTag(item.has("tag") ? item.get("tag").asText() : null);
		team.setLogoUrl(item.has("logo_url") ? item.get("logo_url").asText() : null);

		return team;
	}

	@Override
	protected String getItemTypeDescription() {
		return "team";
	}

}