package com.abe.gg_stats.batch;

import com.abe.gg_stats.entity.Hero;
import com.abe.gg_stats.service.HeroUpdateService;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

/**
 * Processor for Hero entities with improved error handling and validation. Implements
 * circuit breaker pattern and proper exception handling.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class HeroProcessor implements ItemProcessor<JsonNode, Hero> {

	private final HeroUpdateService heroUpdateService;

	@Override
	public Hero process(JsonNode item) throws Exception {
		try {
			log.debug("Processing hero item: {}", item != null ? item.toString() : "null");

			// Validate input
			if (item == null) {
				log.warn("Received null hero item, skipping");
				return null;
			}

			// Validate required fields
			if (!isValidHeroData(item)) {
				log.warn("Invalid hero data received: {}", item.toString());
				return null;
			}

			Hero hero = heroUpdateService.processHeroData(item);

			if (hero == null) {
				log.warn("Hero service returned null for item: {}", item.toString());
				return null;
			}

			log.debug("Successfully processed hero: {} (ID: {})", hero.getName(), hero.getId());
			return hero;

		}
		catch (HeroUpdateService.HeroProcessingException e) {
			log.error("Failed to process hero data: {}", e.getMessage());
			// Re-throw to trigger retry/skip logic
			throw e;
		}
		catch (Exception e) {
			log.error("Unexpected error processing hero: {}", item != null ? item.toString() : "null", e);
			// Re-throw to trigger retry/skip logic
			throw new HeroProcessingException("Failed to process hero data", e);
		}
	}

	/**
	 * Validates that the hero data contains required fields
	 */
	private boolean isValidHeroData(JsonNode item) {
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