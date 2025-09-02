package com.abe.gg_stats.batch.hero;

import com.abe.gg_stats.batch.BaseProcessor;
import com.abe.gg_stats.util.LoggingConstants;
import com.abe.gg_stats.util.LoggingUtils;
import com.abe.gg_stats.util.MDCLoggingContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.abe.gg_stats.dto.HeroDto;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class HeroProcessor extends BaseProcessor<HeroDto> {

	private static final String localizedNameLabel = "localized_name";

	private static final String correlationIdLabel = "correlationId=";

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
		HeroDto dto = new HeroDto(item.get("id").asInt(), item.get("name").asText(),
				item.get("localized_name").asText(), getTextValue(item, "primary_attr").orElse(null),
				getTextValue(item, "attack_type").orElse(null), processRolesArray(item));

		LoggingUtils.logOperationSuccess("HeroProcessor",
				"correlationId=" + MDCLoggingContext.getOrCreateCorrelationId(), dto.name(), String.valueOf(dto.id()));

		return dto;
	}

	@Override
	protected String getItemTypeDescription() {
		return "hero";
	}

	/**
	 * Process roles array from JSON data
	 */
	private List<String> processRolesArray(JsonNode item) {
		final String rolesLabel = "roles";
		if (item.has(rolesLabel) && item.get(rolesLabel).isArray()) {
			return StreamSupport.stream(item.get(rolesLabel).spliterator(), false)
				.filter(role -> role != null && !role.isNull())
				.map(JsonNode::asText)
				.map(String::trim)
				.filter(s -> !s.isEmpty())
				.collect(Collectors.toList());
		}
		return Collections.emptyList();
	}

	/**
	 * Get text value from JSON node, returning null if field is missing or empty
	 */
	private Optional<String> getTextValue(JsonNode node, String fieldName) {
		JsonNode field = node.get(fieldName);
		if (field == null || field.isNull()) {
			return Optional.empty();
		}
		String value = field.asText().trim();
		return value.isEmpty() ? Optional.empty() : Optional.of(value);
	}

}