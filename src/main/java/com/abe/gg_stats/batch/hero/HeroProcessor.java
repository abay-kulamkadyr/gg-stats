package com.abe.gg_stats.batch.hero;

import com.abe.gg_stats.batch.BaseProcessor;
import com.abe.gg_stats.entity.Hero;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Processor for Hero entities with improved error handling and validation. Implements
 * proper exception handling and input validation.
 */
@Component
@Slf4j
public class HeroProcessor extends BaseProcessor<JsonNode, Hero> {

	@Override
	protected boolean isValidInput(JsonNode item) {
		if (item == null) {
			return false;
		}

		// Check for required fields
		if (!item.has("id") || item.get("id").isNull()) {
			log.debug("Hero data missing or null 'id' field");
			return false;
		}

		if (!item.has("name") || item.get("name").isNull()) {
			log.debug("Hero data missing or null 'name' field");
			return false;
		}

		if (!item.has("localized_name") || item.get("localized_name").isNull()) {
			log.debug("Hero data missing or null 'localized_name' field");
			return false;
		}

		// Validate ID is a positive integer
		try {
			int id = item.get("id").asInt();
			if (id <= 0) {
				log.debug("Hero ID must be positive, got: {}", id);
				return false;
			}
		}
		catch (Exception e) {
			log.debug("Hero ID is not a valid integer: {}", item.get("id"));
			return false;
		}

		// Validate name is not empty
		String name = item.get("name").asText();
		if (name == null || name.trim().isEmpty()) {
			log.debug("Hero name is empty or null");
			return false;
		}

		return true;
	}

	@Override
	protected Hero processItem(JsonNode item) throws Exception {
		try {
			Hero hero = new Hero();
			hero.setId(item.get("id").asInt());
			hero.setName(item.get("name").asText());
			hero.setLocalizedName(item.get("localized_name").asText());
			hero.setPrimaryAttr(getTextValue(item, "primary_attr"));
			hero.setAttackType(getTextValue(item, "attack_type"));

			// Process roles array
			List<String> roles = processRolesArray(item);
			hero.setRoles(roles);

			log.debug("Successfully processed hero: {} (ID: {})", hero.getName(), hero.getId());
			return hero;

		}
		catch (Exception e) {
			log.error("Error processing hero data: {}", item.toString(), e);
			throw new HeroProcessingException("Failed to process hero data", e);
		}
	}

	@Override
	protected String getItemTypeDescription() {
		return "hero";
	}

	/**
	 * Process roles array from JSON data
	 */
	private List<String> processRolesArray(JsonNode item) {
		List<String> roles = new ArrayList<>();

		if (item.has("roles") && item.get("roles").isArray()) {
			item.get("roles").forEach(role -> {
				if (role != null && !role.isNull()) {
					String roleText = role.asText();
					if (roleText != null && !roleText.trim().isEmpty()) {
						roles.add(roleText);
					}
				}
			});
		}

		return roles;
	}

	/**
	 * Get text value from JSON node, returning null if field is missing or empty
	 */
	private String getTextValue(JsonNode node, String fieldName) {
		if (!node.has(fieldName) || node.get(fieldName).isNull()) {
			return null;
		}
		String value = node.get(fieldName).asText();
		return (value != null && !value.trim().isEmpty()) ? value : null;
	}

	/**
	 * Custom exception for hero processing errors
	 */
	public static class HeroProcessingException extends Exception {

		public HeroProcessingException(String message) {
			super(message);
		}

		public HeroProcessingException(String message, Throwable cause) {
			super(message, cause);
		}

	}

}