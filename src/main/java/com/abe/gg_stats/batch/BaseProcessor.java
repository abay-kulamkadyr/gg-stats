package com.abe.gg_stats.batch;

import com.abe.gg_stats.util.LoggingUtils;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;

@Slf4j
public abstract class BaseProcessor<JsonNode, O> implements ItemProcessor<JsonNode, O> {

	@Override
	public O process(@NonNull JsonNode item) {
		LoggingUtils.logDebug("Processing item: {}", item);

		// Validate input
		if (!isValidInput(item)) {
			LoggingUtils.logWarning("Invalid input received", "itemType=" + getItemTypeDescription(),
					"item=" + (item != null ? item.toString() : "null"));
			return null;
		}

		// Process the item
		O result = processItem(item);

		if (result == null) {
			LoggingUtils.logWarning("Processing returned null for item", "itemType=" + getItemTypeDescription(),
					"item=" + (item != null ? item.toString() : "null"));
			return null;
		}

		LoggingUtils.logDebug("Processing completed successfully", "result=" + result);
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
