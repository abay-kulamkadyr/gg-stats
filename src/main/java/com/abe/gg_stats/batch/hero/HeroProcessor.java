package com.abe.gg_stats.batch.hero;

import com.abe.gg_stats.batch.BaseProcessor;
import com.abe.gg_stats.dto.HeroDto;
import com.abe.gg_stats.util.LoggingConstants;
import com.abe.gg_stats.util.LoggingUtils;
import com.abe.gg_stats.util.MDCLoggingContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class HeroProcessor extends BaseProcessor<HeroDto> {

	private static final String localizedNameLabel = "localized_name";

	private static final String correlationIdLabel = "correlationId=";

	private final ObjectMapper objectMapper;

	@Override
	protected boolean isValidInput(@NonNull JsonNode item) {
		// Set up validation context
		String correlationId = MDCLoggingContext.getOrCreateCorrelationId();
		MDCLoggingContext.updateContext("operationType", LoggingConstants.OPERATION_TYPE_BATCH);
		MDCLoggingContext.updateContext("batchType", "heroes");

		// Check for required fields
		if (!item.has("id") || item.get("id").isNull()) {
			LoggingUtils.logWarning(LoggingConstants.JSON_PARSING_ERROR, correlationIdLabel + correlationId,
					"Hero data is missing or null 'id' field");
			return false;
		}

		if (!item.has("name") || item.get("name").isNull()) {
			LoggingUtils.logWarning(LoggingConstants.JSON_PARSING_ERROR, correlationIdLabel + correlationId,
					"Hero data missing or null 'name' field");
			return false;
		}

		if (!item.has(localizedNameLabel) || item.get(localizedNameLabel).isNull()) {
			LoggingUtils.logWarning(LoggingConstants.JSON_PARSING_ERROR, correlationIdLabel + correlationId,
					"Hero data missing or null 'localized_name' field");
			return false;
		}

		// Validate ID is a positive integer
		try {
			int id = item.get("id").asInt();
			if (id <= 0) {
				LoggingUtils.logWarning(LoggingConstants.JSON_PARSING_ERROR, correlationIdLabel + correlationId,
						"Hero ID must be positive, got: " + id);
				return false;
			}
		}
		catch (Exception e) {
			LoggingUtils.logWarning(LoggingConstants.JSON_PARSING_ERROR, correlationIdLabel + correlationId,
					"Hero ID is not a valid integer");
			return false;
		}

		// Validate name is not empty
		String name = item.get("name").asText();
		if (name == null || name.trim().isEmpty()) {
			LoggingUtils.logWarning(LoggingConstants.JSON_PARSING_ERROR, correlationIdLabel + correlationId,
					"Hero name is empty or null 'name' field");
			return false;
		}

		return true;
	}

	@Override
	protected HeroDto processItem(@NonNull JsonNode item) {
		HeroDto dto;
		try {
			dto = objectMapper.treeToValue(item, HeroDto.class);
		}
		catch (Exception e) {
			LoggingUtils.logWarning("HeroProcessor", LoggingConstants.JSON_PARSING_ERROR,
					"correlationId=" + MDCLoggingContext.getOrCreateCorrelationId());
			return null;
		}

		// Normalize roles: ensure non-null, trim and remove blanks/nulls
		java.util.List<String> roles = dto.roles() == null ? java.util.Collections.emptyList()
				: dto.roles()
					.stream()
					.filter(Objects::nonNull)
					.map(String::trim)
					.filter(s -> !s.isEmpty())
					.collect(java.util.stream.Collectors.toList());

		dto = new HeroDto(dto.id(), dto.name(), dto.localizedName(), dto.primaryAttr(), dto.attackType(), roles);

		LoggingUtils.logOperationSuccess("HeroProcessor",
				"correlationId=" + MDCLoggingContext.getOrCreateCorrelationId(), dto.name(), String.valueOf(dto.id()));

		return dto;
	}

	@Override
	protected String getItemTypeDescription() {
		return "hero";
	}

}