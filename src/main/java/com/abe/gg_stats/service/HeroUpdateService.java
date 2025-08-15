package com.abe.gg_stats.service;

import com.abe.gg_stats.entity.Hero;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class HeroUpdateService {

	public Hero processHeroData(JsonNode item) {
		try {
			validateHeroData(item);

			Hero hero = new Hero();
			hero.setId(getRequiredIntValue(item, "id"));
			hero.setName(getRequiredTextValue(item, "name"));
			hero.setLocalizedName(getRequiredTextValue(item, "localized_name"));
			hero.setPrimaryAttr(getTextValue(item, "primary_attr"));
			hero.setAttackType(getTextValue(item, "attack_type"));

			// Process roles array
			List<String> roles = processRolesArray(item);
			hero.setRoles(roles);

			log.debug("Successfully processed hero: {} (ID: {})", hero.getName(), hero.getId());
			return hero;

		}
		catch (HeroProcessingException e) {
			// Re-throw validation exceptions as-is
			throw e;
		}
		catch (Exception e) {
			log.error("Error processing hero data: {}", item != null ? item.toString() : "null", e);
			throw new HeroProcessingException("Failed to process hero data", e);
		}
	}

	private void validateHeroData(JsonNode item) {
		if (item == null) {
			throw new HeroProcessingException("Hero data is null");
		}

		if (!item.has("id") || item.get("id").isNull()) {
			throw new HeroProcessingException("Hero ID is required");
		}

		if (!item.has("name") || item.get("name").isNull()) {
			throw new HeroProcessingException("Hero name is required");
		}

		if (!item.has("localized_name") || item.get("localized_name").isNull()) {
			throw new HeroProcessingException("Hero localized name is required");
		}
	}

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

	private String getRequiredTextValue(JsonNode node, String fieldName) {
		if (!node.has(fieldName) || node.get(fieldName).isNull()) {
			throw new HeroProcessingException("Required field '" + fieldName + "' is missing or null");
		}
		return node.get(fieldName).asText();
	}

	private Integer getRequiredIntValue(JsonNode node, String fieldName) {
		if (!node.has(fieldName) || node.get(fieldName).isNull()) {
			throw new HeroProcessingException("Required field '" + fieldName + "' is missing or null");
		}
		return node.get(fieldName).asInt();
	}

	private String getTextValue(JsonNode node, String fieldName) {
		if (!node.has(fieldName) || node.get(fieldName).isNull()) {
			return null;
		}
		String value = node.get(fieldName).asText();
		return (value != null && !value.trim().isEmpty()) ? value : null;
	}

	public static class HeroProcessingException extends RuntimeException {

		public HeroProcessingException(String message) {
			super(message);
		}

		public HeroProcessingException(String message, Throwable cause) {
			super(message, cause);
		}

	}

}
