package com.abe.gg_stats.batch;

import com.abe.gg_stats.entity.NotablePlayer;
import com.abe.gg_stats.entity.Team;
import com.abe.gg_stats.repository.NotablePlayerRepository;
import com.abe.gg_stats.repository.TeamRepository;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Processor for NotablePlayer entities with improved error handling and validation.
 * Implements proper exception handling and input validation.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NotablePlayerProcessor implements ItemProcessor<JsonNode, NotablePlayer> {

	private final NotablePlayerRepository notablePlayerRepository;

	private final TeamRepository teamRepository;

	@Override
	public NotablePlayer process(JsonNode item) throws Exception {
		try {
			// Validate input
			if (item == null) {
				log.warn("Received null item, skipping");
				return null;
			}

			if (!isValidNotablePlayerData(item)) {
				log.warn("Invalid notable player data received: {}", item.toString());
				return null;
			}

			Long accountId = item.get("account_id").asLong();

			// First try to find existing NotablePlayer
			Optional<NotablePlayer> existingNotablePlayer = notablePlayerRepository.findById(accountId);

			if (existingNotablePlayer.isPresent()) {
				// Update existing NotablePlayer
				NotablePlayer notablePlayer = existingNotablePlayer.get();
				updateNotablePlayerFields(notablePlayer, item);
				return notablePlayer;
			}

			// Create new NotablePlayer if not found
			NotablePlayer notablePlayer = new NotablePlayer();
			notablePlayer.setAccountId(accountId);
			updateNotablePlayerFields(notablePlayer, item);

			return notablePlayer;
		}
		catch (Exception e) {
			log.error("Error processing notable player: {}", item != null ? item.toString() : "null", e);
			throw new NotablePlayerProcessingException("Failed to process notable player data", e);
		}
	}

	/**
	 * Validates that the notable player data contains required fields
	 */
	private boolean isValidNotablePlayerData(JsonNode item) {
		if (item == null) {
			return false;
		}

		// Check for required fields
		if (!item.has("account_id") || item.get("account_id").isNull()) {
			log.debug("Notable player data missing or null 'account_id' field");
			return false;
		}

		// Validate account_id is a positive long
		try {
			long accountId = item.get("account_id").asLong();
			if (accountId <= 0) {
				log.debug("Notable player account_id must be positive, got: {}", accountId);
				return false;
			}
		}
		catch (Exception e) {
			log.debug("Notable player account_id is not a valid long: {}", item.get("account_id"));
			return false;
		}

		return true;
	}

	/**
	 * Updates notable player fields from JSON data
	 */
	private void updateNotablePlayerFields(NotablePlayer notablePlayer, JsonNode item) {
		notablePlayer.setName(item.has("name") ? item.get("name").asText() : null);
		notablePlayer.setCountryCode(item.has("country_code") ? item.get("country_code").asText() : null);
		notablePlayer.setFantasyRole(item.has("fantasy_role") ? item.get("fantasy_role").asInt() : null);
		notablePlayer.setIsLocked(item.has("is_locked") ? item.get("is_locked").asBoolean() : false);
		notablePlayer.setIsPro(item.has("is_pro") ? item.get("is_pro").asBoolean() : true);

		// Handle team association
		if (item.has("team_id") && !item.get("team_id").isNull()) {
			try {
				Long teamId = item.get("team_id").asLong();
				if (teamId > 0) {
					Optional<Team> teamOpt = teamRepository.findById(teamId);
					teamOpt.ifPresent(notablePlayer::setTeam);
				}
			}
			catch (Exception e) {
				log.warn("Invalid team_id in notable player data: {}", item.get("team_id"));
			}
		}
	}

	/**
	 * Custom exception for notable player processing errors
	 */
	public static class NotablePlayerProcessingException extends Exception {

		public NotablePlayerProcessingException(String message) {
			super(message);
		}

		public NotablePlayerProcessingException(String message, Throwable cause) {
			super(message, cause);
		}

	}

}