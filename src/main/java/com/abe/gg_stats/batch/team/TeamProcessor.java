package com.abe.gg_stats.batch.team;

import com.abe.gg_stats.batch.BaseProcessor;
import com.abe.gg_stats.dto.TeamDto;
import com.abe.gg_stats.entity.Team;
import com.abe.gg_stats.util.LoggingConstants;
import com.abe.gg_stats.util.LoggingUtils;
import com.abe.gg_stats.util.MDCLoggingContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

/**
 * Processor for Team entities with improved error handling and validation. Implements
 * proper exception handling and input validation.
 */
@Component
public class TeamProcessor extends BaseProcessor<TeamDto> {

	private final ObjectMapper objectMapper;

	public TeamProcessor(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

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
	protected TeamDto processItem(JsonNode item) {
		// Set up processing context
		MDCLoggingContext.updateContext("operationType", LoggingConstants.OPERATION_TYPE_BATCH);
		MDCLoggingContext.updateContext("batchType", "teams");

		try {
			return objectMapper.treeToValue(item, TeamDto.class);
		}
		catch (Exception e) {
			return null;
		}
	}

	@Override
	protected String getItemTypeDescription() {
		return "team";
	}

}