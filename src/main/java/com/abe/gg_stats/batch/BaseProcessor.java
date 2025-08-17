package com.abe.gg_stats.batch;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;

@Slf4j
public abstract class BaseProcessor<JsonNode, O> implements ItemProcessor<JsonNode, O> {

	@Override
	public O process(@NonNull JsonNode item) {
		log.debug("Processing item: {}", item);

		// Validate input
		if (!isValidInput(item)) {
			log.warn("Invalid input received: {}", item);
			return null;
		}

		// Process the item
		O result = processItem(item);

		if (result == null) {
			log.warn("Processing returned null for item: {}", item);
			return null;
		}
		return result;
	}

	/**
	 * Validate the input item
	 */
	protected abstract boolean isValidInput(JsonNode item);

	/**
	 * Process the validated item
	 */
	protected abstract O processItem(JsonNode item);

	/**
	 * Get a description of the item type for logging
	 */
	protected abstract String getItemTypeDescription();

}
