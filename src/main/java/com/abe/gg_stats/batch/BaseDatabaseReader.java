package com.abe.gg_stats.batch;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemReader;

import java.util.Iterator;

@Slf4j
public abstract class BaseDatabaseReader<T> implements ItemReader<T> {

	protected Iterator<T> dataIterator;

	protected boolean initialized = false;

	@Override
	public T read() throws Exception {
		if (!initialized) {
			initialize();
		}

		if (dataIterator != null && dataIterator.hasNext()) {
			return dataIterator.next();
		}

		return null; // End of data
	}

	protected abstract void initialize();

	/**
	 * Get a description of the data type for logging
	 */
	protected abstract String getDataTypeDescription();

}
