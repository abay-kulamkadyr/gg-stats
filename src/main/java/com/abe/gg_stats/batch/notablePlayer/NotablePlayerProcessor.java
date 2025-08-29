package com.abe.gg_stats.batch.notablePlayer;

import com.abe.gg_stats.batch.BaseProcessor;
import com.abe.gg_stats.entity.NotablePlayer;
import com.abe.gg_stats.entity.Team;
import com.abe.gg_stats.repository.NotablePlayerRepository;
import com.abe.gg_stats.repository.TeamRepository;
import com.abe.gg_stats.util.LoggingConstants;
import com.abe.gg_stats.util.LoggingUtils;
import com.abe.gg_stats.util.MDCLoggingContext;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Processor for NotablePlayer entities with improved error handling and validation.
 * Implements proper exception handling and input validation.
 */
@Component
public class NotablePlayerProcessor extends BaseProcessor<JsonNode, NotablePlayer> {

	private final NotablePlayerRepository notablePlayerRepository;

	private final TeamRepository teamRepository;

	public NotablePlayerProcessor(NotablePlayerRepository notablePlayerRepository, TeamRepository teamRepository) {
		this.notablePlayerRepository = notablePlayerRepository;
		this.teamRepository = teamRepository;
	}

	@Override
	protected boolean isValidInput(JsonNode item) {
		// Set up validation context
		String correlationId = MDCLoggingContext.getOrCreateCorrelationId();
		MDCLoggingContext.updateContext("operationType", LoggingConstants.OPERATION_TYPE_BATCH);
		MDCLoggingContext.updateContext("batchType", "notableplayers");
		
		if (item == null) {
			return false;
		}

		// Check for required fields
		if (!item.has("account_id") || item.get("account_id").isNull()) {
			LoggingUtils.logDebug("Notable player data missing or null 'account_id' field", 
				"correlationId=" + correlationId);
			return false;
		}

		// Validate account_id is a positive long
		try {
			long accountId = item.get("account_id").asLong();
			if (accountId <= 0) {
				LoggingUtils.logDebug("Notable player account_id must be positive, got: {}", 
					"correlationId=" + correlationId,
					"accountId=" + accountId);
				return false;
			}
		}
		catch (Exception e) {
			LoggingUtils.logDebug("Notable player account_id is not a valid long: {}", 
				"correlationId=" + correlationId,
				"accountId=" + item.get("account_id"));
			return false;
		}

		return true;
	}

	@Override
	protected NotablePlayer processItem(JsonNode item) {
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

	@Override
	protected String getItemTypeDescription() {
		return "notable player";
	}

	/**
	 * Updates notable player fields from JSON data
	 */
	private void updateNotablePlayerFields(NotablePlayer notablePlayer, JsonNode item) {
		// Set up processing context
		String correlationId = MDCLoggingContext.getOrCreateCorrelationId();
		MDCLoggingContext.updateContext("operationType", LoggingConstants.OPERATION_TYPE_BATCH);
		MDCLoggingContext.updateContext("batchType", "notableplayers");
		
		notablePlayer.setName(item.has("name") ? item.get("name").asText() : null);
		notablePlayer.setCountryCode(item.has("country_code") ? item.get("country_code").asText() : null);
		notablePlayer.setFantasyRole(item.has("fantasy_role") ? item.get("fantasy_role").asInt() : null);
		notablePlayer.setIsLocked(item.has("is_locked") && item.get("is_locked").asBoolean());
		notablePlayer.setIsPro(!item.has("is_pro") || item.get("is_pro").asBoolean());

		// Handle team association
		if (item.has("team_id") && !item.get("team_id").isNull()) {
			try {
				long teamId = item.get("team_id").asLong();
				if (teamId > 0) {
					Optional<Team> teamOpt = teamRepository.findById(teamId);
					teamOpt.ifPresent(notablePlayer::setTeam);
				}
			}
			catch (Exception e) {
				LoggingUtils.logWarning("Invalid team_id in notable player data", 
					"correlationId=" + correlationId,
					"teamId=" + item.get("team_id"));
			}
		}
	}

}