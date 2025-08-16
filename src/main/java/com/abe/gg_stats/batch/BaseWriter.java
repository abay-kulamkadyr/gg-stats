package com.abe.gg_stats.batch;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;

@Slf4j
public abstract class BaseWriter<T> implements ItemWriter<T> {

	@Override
	public void write(Chunk<? extends T> chunk) throws Exception {
		if (chunk == null || chunk.isEmpty()) {
			log.debug("Empty chunk received, nothing to write");
			return;
		}

		log.info("Writing {} items to database", chunk.size());

		int successCount = 0;
		int errorCount = 0;

		for (T item : chunk) {
			try {
				writeItem(item);
				successCount++;
				log.debug("Successfully wrote item: {}", item.toString());
			}
			catch (Exception e) {
				errorCount++;
				log.error("Error writing item: {}", item != null ? item.toString() : "null", e);
				// Continue processing other items
			}
		}

		log.info("Write operation completed: {} successful, {} errors", successCount, errorCount);

		if (errorCount > 0) {
			log.warn("Some items failed to write. Check logs for details.");
		}
	}

	/**
	 * Write a single item to the database
	 */
	protected abstract void writeItem(T item) throws Exception;

	/**
	 * Get a description of the item type for logging
	 */
	protected abstract String getItemTypeDescription();

}
