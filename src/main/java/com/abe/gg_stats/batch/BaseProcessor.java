package com.abe.gg_stats.batch;

import com.abe.gg_stats.util.LoggingConstants;
import com.abe.gg_stats.util.LoggingUtils;
import com.abe.gg_stats.util.MDCLoggingContext;
import lombok.NonNull;
import org.springframework.batch.item.ItemProcessor;

public abstract class BaseProcessor<JsonNode, O> implements ItemProcessor<JsonNode, O> {

	@Override
	public O process(@NonNull JsonNode item) {
		// Set up processing context - inherit existing context if available
		String existingCorrelationId = MDCLoggingContext.getCurrentCorrelationId();
		String correlationId = existingCorrelationId != null ? existingCorrelationId : MDCLoggingContext.getOrCreateCorrelationId();
		
		// Update context with batch-specific information
		MDCLoggingContext.updateContext("operationType", LoggingConstants.OPERATION_TYPE_BATCH);
		MDCLoggingContext.updateContext("batchType", getItemTypeDescription());
		
		LoggingUtils.logDebug("Processing " + getItemTypeDescription() + " item", 
			"itemType=" + getItemTypeDescription(),
			"correlationId=" + correlationId,
			"item=" + (item != null ? item.toString() : "null"));

		// Validate input
		if (!isValidInput(item)) {
			LoggingUtils.logWarning("Invalid " + getItemTypeDescription() + " input received", 
				"itemType=" + getItemTypeDescription(),
				"correlationId=" + correlationId,
				"item=" + (item != null ? item.toString() : "null"));
			return null;
		}

		// Process the item
		O result = processItem(item);

		if (result == null) {
			LoggingUtils.logWarning("Processing returned null for " + getItemTypeDescription(), 
				"itemType=" + getItemTypeDescription(),
				"correlationId=" + correlationId,
				"item=" + (item != null ? item.toString() : "null"));
			return null;
		}

		LoggingUtils.logDebug("Processing completed successfully", 
			"itemType=" + getItemTypeDescription(),
			"correlationId=" + correlationId,
			"result=" + result);
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
