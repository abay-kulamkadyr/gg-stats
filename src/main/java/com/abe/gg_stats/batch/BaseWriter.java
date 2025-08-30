package com.abe.gg_stats.batch;

import com.abe.gg_stats.util.LoggingConstants;
import com.abe.gg_stats.util.LoggingUtils;
import com.abe.gg_stats.util.MDCLoggingContext;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;

@Slf4j
public abstract class BaseWriter<T> implements ItemWriter<T> {

	@Override
	public void write(@NonNull Chunk<? extends T> chunk) {
		// Set up writing context
		String correlationId = MDCLoggingContext.getOrCreateCorrelationId();
		MDCLoggingContext.updateContext("operationType", LoggingConstants.OPERATION_TYPE_BATCH);
		MDCLoggingContext.updateContext("batchType", getItemTypeDescription());

		if (chunk.isEmpty()) {
			LoggingUtils.logDebug("Empty chunk received, nothing to write", "itemType=" + getItemTypeDescription(),
					"correlationId=" + correlationId);
			return;
		}

		final String itemLabel = "items=";
		final String itemTypeLabel = "itemType";
		final String correlationIdLabel = "correlationId=";
		LoggingUtils.logOperationStart("batch write operation", itemTypeLabel + getItemTypeDescription(),
				correlationIdLabel + correlationId, itemLabel + chunk.size());

		int successCount = 0;
		int errorCount = 0;

		for (T item : chunk) {
			try {
				writeItem(item);
				successCount++;
				LoggingUtils.logDebug("Successfully wrote " + getItemTypeDescription() + " item",
						itemTypeLabel + getItemTypeDescription(), correlationIdLabel + correlationId,
						itemLabel + item.toString());
			}
			catch (Exception e) {
				errorCount++;
				LoggingUtils.logOperationFailure("item write", "Failed to write " + getItemTypeDescription(), e,
						itemTypeLabel + getItemTypeDescription(), correlationIdLabel + correlationId,
						"chunkSize=" + chunk.size(), "itemIndex=" + (successCount + errorCount),
						"errorType=" + e.getClass().getSimpleName());
			}
		}

		LoggingUtils.logOperationSuccess("batch write operation", itemTypeLabel + getItemTypeDescription(),
				correlationId + correlationId, "successful=" + successCount, "errors=" + errorCount);

		if (errorCount > 0) {
			LoggingUtils.logWarning("Some " + getItemTypeDescription() + " items failed to write",
					itemTypeLabel + getItemTypeDescription(), correlationIdLabel + correlationId,
					"errorCount=" + errorCount);
		}
	}

	/**
	 * Write a single item to the database
	 */
	protected abstract void writeItem(T item);

	/**
	 * Get a description of the item type for logging
	 */
	protected abstract String getItemTypeDescription();

}
