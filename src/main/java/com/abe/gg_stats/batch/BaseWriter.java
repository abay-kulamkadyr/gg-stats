package com.abe.gg_stats.batch;

import com.abe.gg_stats.util.LoggingUtils;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;

@Slf4j
public abstract class BaseWriter<T> implements ItemWriter<T> {

	@Override
	public void write(@NonNull Chunk<? extends T> chunk) {
		if (chunk.isEmpty()) {
			LoggingUtils.logDebug("Empty chunk received, nothing to write");
			return;
		}

		LoggingUtils.logOperationStart("batch write operation", "items=" + chunk.size());

		int successCount = 0;
		int errorCount = 0;

		for (T item : chunk) {
			try {
				writeItem(item);
				successCount++;
				LoggingUtils.logDebug("Successfully wrote item: {}", item.toString());
			}
			catch (Exception e) {
				errorCount++;
				LoggingUtils.logOperationFailure("item write", "Failed to write item", e,
						"itemType=" + getItemTypeDescription(), "chunkIndex=" + chunk.size(),
						"itemIndex=" + (successCount + errorCount), "errorType=" + e.getClass().getSimpleName());
			}
		}

		LoggingUtils.logOperationSuccess("batch write operation", "successful=" + successCount, "errors=" + errorCount);

		if (errorCount > 0) {
			LoggingUtils.logWarning("Some items failed to write", "errorCount=" + errorCount);
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
