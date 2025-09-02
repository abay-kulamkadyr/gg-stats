package com.abe.gg_stats.batch.notable_player;

import com.abe.gg_stats.batch.BaseProcessor;
import com.abe.gg_stats.dto.NotablePlayerDto;
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
 * Processor for NotablePlayer with improved error handling and validation.
 */
@Component
public class NotablePlayerProcessor extends BaseProcessor<NotablePlayerDto> {

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
						"correlationId=" + correlationId, "accountId=" + accountId);
				return false;
			}
		}
		catch (Exception e) {
			LoggingUtils.logDebug("Notable player account_id is not a valid long: {}", "correlationId=" + correlationId,
					"accountId=" + item.get("account_id"));
			return false;
		}

		return true;
	}

	@Override
	protected NotablePlayerDto processItem(JsonNode item) {
		Long accountId = item.get("account_id").asLong();
		String name = item.has("name") ? item.get("name").asText() : null;
		String countryCode = item.has("country_code") ? item.get("country_code").asText() : null;
		Integer fantasyRole = item.has("fantasy_role") ? item.get("fantasy_role").asInt() : null;
		Boolean isLocked = item.has("is_locked") && item.get("is_locked").asBoolean();
		Boolean isPro = !item.has("is_pro") || item.get("is_pro").asBoolean();

		Long teamId = null;
		if (item.has("team_id") && !item.get("team_id").isNull()) {
			try {
				long parsedTeamId = item.get("team_id").asLong();
				if (parsedTeamId > 0) {
					teamId = parsedTeamId;
				}
			}
			catch (Exception e) {
				// ignore, will return null teamId
			}
		}

		return new NotablePlayerDto(accountId, countryCode, fantasyRole, name, isLocked, isPro, teamId);
	}

	@Override
	protected String getItemTypeDescription() {
		return "notable player";
	}

	/**
	 * Legacy helper used for entity path retained for reference.
	 */
	private void updateNotablePlayerFields(NotablePlayer notablePlayer, JsonNode item) {
		// No-op in DTO processor
	}

}