package com.abe.gg_stats.batch;

import com.abe.gg_stats.entity.Team;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

/**
 * Processor for Team entities with improved error handling and validation. Implements
 * proper exception handling and input validation.
 */
@Component
@Slf4j
public class TeamProcessor implements ItemProcessor<JsonNode, Team> {

	@Override
	public Team process(JsonNode item) throws Exception {
		try {
			// Validate input
			if (item == null) {
				log.warn("Received null item, skipping");
				return null;
			}

			if (!isValidTeamData(item)) {
				log.warn("Invalid team data received: {}", item.toString());
				return null;
			}

			Team team = new Team();
			team.setTeamId(item.get("team_id").asLong());
			JsonNode ratingNode = item.get("rating");
			log.debug("Processing rating field: has={}, node={}, isNull={}, isNumber={}", item.has("rating"),
					ratingNode, ratingNode != null ? ratingNode.isNull() : "N/A",
					ratingNode != null ? ratingNode.isNumber() : "N/A");
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
		catch (Exception e) {
			log.error("Error processing team: {}", item != null ? item.toString() : "null", e);
			throw new TeamProcessingException("Failed to process team data", e);
		}
	}

	/**
	 * Validates that the team data contains required fields
	 */
	private boolean isValidTeamData(JsonNode item) {
		if (item == null) {
			return false;
		}

		// Check for required fields
		if (!item.has("team_id") || item.get("team_id").isNull()) {
			log.debug("Team data missing or null 'team_id' field");
			return false;
		}

		// Validate team_id is a positive long
		try {
			long teamId = item.get("team_id").asLong();
			if (teamId <= 0) {
				log.debug("Team team_id must be positive, got: {}", teamId);
				return false;
			}
		}
		catch (Exception e) {
			log.debug("Team team_id is not a valid long: {}", item.get("team_id"));
			return false;
		}

		return true;
	}

	/**
	 * Custom exception for team processing errors
	 */
	public static class TeamProcessingException extends Exception {

		public TeamProcessingException(String message) {
			super(message);
		}

		public TeamProcessingException(String message, Throwable cause) {
			super(message, cause);
		}

	}

}