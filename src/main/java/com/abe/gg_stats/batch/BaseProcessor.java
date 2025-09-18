package com.abe.gg_stats.batch;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.lang.NonNull;

public abstract class BaseProcessor<O> implements ItemProcessor<JsonNode, O> {

	@Override
	public O process(@NonNull JsonNode item) {
		if (!isValidInput(item)) {
			return null;
		}
		return processItem(item);
	}

	/**
	 * Validate the input item (the most essential fields for database integrity)
	 */
	protected abstract boolean isValidInput(JsonNode item);

	/**
	 * Process the validated item
	 */
	protected abstract O processItem(JsonNode item);

}
