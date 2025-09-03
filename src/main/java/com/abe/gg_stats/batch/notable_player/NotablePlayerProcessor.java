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
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Processor for NotablePlayer with improved error handling and validation.
 */
@Component
@AllArgsConstructor
public class NotablePlayerProcessor extends BaseProcessor<NotablePlayerDto> {

	private final ObjectMapper objectMapper;

	@Override
	public boolean isValidInput(JsonNode item) {
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
		NotablePlayerDto dto;
		try {
			dto = objectMapper.treeToValue(item, NotablePlayerDto.class);
		}
		catch (JsonProcessingException e) {
			return null;
		}
		return dto;
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