package com.abe.gg_stats.batch.hero;

import com.abe.gg_stats.batch.BaseProcessor;
import com.abe.gg_stats.entity.Hero;
import com.abe.gg_stats.util.LoggingUtils;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import lombok.NonNull;
import org.springframework.stereotype.Component;

@Component
public class HeroProcessor extends BaseProcessor<JsonNode, Hero> {

	@Override
	protected boolean isValidInput(@NonNull JsonNode item) {

		// Check for required fields
		String LOG_PARSING_ERROR = "HeroProcessor Json validation error";
		if (!item.has("id") || item.get("id").isNull()) {
			LoggingUtils.logWarning(LOG_PARSING_ERROR, "Hero data is missing or null 'id' field");
			return false;
		}

		if (!item.has("name") || item.get("name").isNull()) {
			LoggingUtils.logWarning("HeroProcessor", "Hero data missing or null 'name' field");
			return false;
		}

		if (!item.has("localized_name") || item.get("localized_name").isNull()) {
			LoggingUtils.logWarning(LOG_PARSING_ERROR, "Hero data missing or null 'localized_name' field");
			return false;
		}

		// Validate ID is a positive integer
		try {
			int id = item.get("id").asInt();
			if (id <= 0) {
				LoggingUtils.logWarning(LOG_PARSING_ERROR, "Hero ID must be positive, got: " + id);
				return false;
			}
		}
		catch (Exception e) {
			LoggingUtils.logWarning(LOG_PARSING_ERROR, "Hero ID is not a valid integer");
			return false;
		}

		// Validate name is not empty
		String name = item.get("name").asText();
		if (name == null || name.trim().isEmpty()) {
			LoggingUtils.logWarning(LOG_PARSING_ERROR, "Hero name is empty or null 'name' field");
			return false;
		}

		return true;
	}

	@Override
	protected Hero processItem(@NonNull JsonNode item) {
		Hero hero = new Hero();

		// Required fields
		hero.setId(item.get("id").asInt());
		hero.setName(item.get("name").asText());
		hero.setLocalizedName(item.get("localized_name").asText());

		// Optional fields
		hero.setPrimaryAttr(getTextValue(item, "primary_attr").orElse(null));
		hero.setAttackType(getTextValue(item, "attack_type").orElse(null));

		// Roles array
		List<String> roles = processRolesArray(item);
		hero.setRoles(roles);

		LoggingUtils.logOperationSuccess("HeroProcessor", hero.getName(), hero.getId());
		return hero;
	}

	@Override
	protected String getItemTypeDescription() {
		return "hero";
	}

	/**
	 * Process roles array from JSON data
	 */
	private List<String> processRolesArray(JsonNode item) {
		if (item.has("roles") && item.get("roles").isArray()) {
			return StreamSupport.stream(item.get("roles").spliterator(), false)
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