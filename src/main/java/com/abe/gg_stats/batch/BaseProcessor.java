package com.abe.gg_stats.batch;

import com.abe.gg_stats.util.LoggingConstants;
import com.abe.gg_stats.util.LoggingUtils;
import com.abe.gg_stats.util.MDCLoggingContext;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.NonNull;
import org.springframework.batch.item.ItemProcessor;

public abstract class BaseProcessor<O> implements ItemProcessor<JsonNode, O> {

	@Override
	public O process(@NonNull JsonNode item) {
		// Set up processing context - inherit existing context if available
		String existingCorrelationId = MDCLoggingContext.getCurrentCorrelationId();
		String correlationId = existingCorrelationId != null ? existingCorrelationId
				: MDCLoggingContext.getOrCreateCorrelationId();

		// Update context with batch-specific information
		MDCLoggingContext.updateContext("operationType", LoggingConstants.OPERATION_TYPE_BATCH);
		MDCLoggingContext.updateContext("batchType", getItemTypeDescription());

		final String itemLabel = "item=";
		final String itemTypeLabel = "itemType=";
		final String correlationIdLabel = "correlationId=";

		LoggingUtils.logDebug("Processing " + getItemTypeDescription() + " item",
				itemTypeLabel + getItemTypeDescription(), correlationIdLabel + correlationId, itemLabel + item);

		// Validate input
		if (!isValidInput(item)) {
			LoggingUtils.logWarning("Invalid " + getItemTypeDescription() + " input received",
					itemTypeLabel + getItemTypeDescription(), correlationIdLabel + correlationId, itemLabel + item);
			return null;
		}

		// Process the item
		O result = processItem(item);

		if (result == null) {
			LoggingUtils.logWarning("Processing returned null for " + getItemTypeDescription(),
					itemTypeLabel + getItemTypeDescription(), correlationIdLabel + correlationId, itemLabel + item);
			return null;
		}

		LoggingUtils.logDebug("Processing completed successfully", itemTypeLabel + getItemTypeDescription(),
				correlationIdLabel + correlationId, "result=" + result);
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
