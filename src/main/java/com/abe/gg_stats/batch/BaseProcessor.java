package com.abe.gg_stats.batch;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;

@Slf4j
public abstract class BaseProcessor<I, O> implements ItemProcessor<I, O> {

	@Override
	public O process(I item) throws Exception {
		try {
			log.debug("Processing item: {}", item != null ? item.toString() : "null");

			// Validate input
			if (!isValidInput(item)) {
				log.warn("Invalid input received: {}", item != null ? item.toString() : "null");
				return null;
			}

			// Process the item
			O result = processItem(item);

			if (result == null) {
				log.warn("Processing returned null for item: {}", item != null ? item.toString() : "null");
				return null;
			}

			assert item != null;
			log.debug("Successfully processed item: {}", item);
			return result;

		}
		catch (Exception e) {
			log.error("Error processing item: {}", item != null ? item.toString() : "null", e);
			throw e;
		}
	}

	/**
	 * Validate the input item
	 */
	protected abstract boolean isValidInput(I item);

	/**
	 * Process the validated item
	 */
	protected abstract O processItem(I item) throws Exception;

	/**
	 * Get a description of the item type for logging
	 */
	protected abstract String getItemTypeDescription();

}
