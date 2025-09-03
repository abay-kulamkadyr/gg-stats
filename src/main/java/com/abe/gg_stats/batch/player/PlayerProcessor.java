package com.abe.gg_stats.batch.player;

import com.abe.gg_stats.batch.BaseProcessor;
import com.abe.gg_stats.dto.PlayerDto;
import com.abe.gg_stats.dto.mapper.PlayerResponseMapper;
import com.abe.gg_stats.dto.response.PlayerResponseDto;
import com.abe.gg_stats.util.LoggingConstants;
import com.abe.gg_stats.util.LoggingUtils;
import com.abe.gg_stats.util.MDCLoggingContext;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class PlayerProcessor extends BaseProcessor<PlayerDto> {

	private final ObjectMapper objectMapper;

	private final PlayerResponseMapper playerResponseMapper;

	@Override
	protected boolean isValidInput(JsonNode item) {
		// Set up validation context
		String correlationId = MDCLoggingContext.getOrCreateCorrelationId();
		MDCLoggingContext.updateContext("operationType", LoggingConstants.OPERATION_TYPE_BATCH);
		MDCLoggingContext.updateContext("batchType", "players");

		if (item == null || item.isNull()) {
			LoggingUtils.logWarning("Player JSON item is null", "correlationId=" + correlationId);
			return false;
		}

		// Optional: check that at least one identifier exists
		boolean hasAccountId = item.has("account_id") && !item.get("account_id").isNull();
		boolean hasProfile = item.has("profile") && item.get("profile").isObject();

		if (!hasAccountId && !hasProfile) {
			LoggingUtils.logWarning("Player JSON item missing both account_id and profile",
					"correlationId=" + correlationId, "item=" + item);
			return false;
		}

		if (hasProfile) {
			JsonNode profile = item.get("profile");

			// Validate required profile strings if present
			String steamId = getTextValue(profile, "steamid");
			String personName = getTextValue(profile, "personaname");

			if (steamId == null || personName == null) {
				LoggingUtils.logWarning("Invalid profile data, missing required fields",
						"correlationId=" + correlationId, "profile=" + profile);
				return false;
			}
		}

		return true;
	}

	@Override
	protected PlayerDto processItem(JsonNode item) {
		PlayerResponseDto dto;
		try {
			dto = objectMapper.treeToValue(item, PlayerResponseDto.class);
		}
		catch (JsonProcessingException e) {
			return null;
		}
		return playerResponseMapper.toPlayerDto(dto);
	}

	@Override
	protected String getItemTypeDescription() {
		return "player account ID";
	}

	private String getTextValue(JsonNode node, String fieldName) {
		if (!node.has(fieldName) || node.get(fieldName).isNull()) {
			return null;
		}
		String value = node.get(fieldName).asText();
		return (value != null && !value.trim().isEmpty()) ? value : null;
	}

}