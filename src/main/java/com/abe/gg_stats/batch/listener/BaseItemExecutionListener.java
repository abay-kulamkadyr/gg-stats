package com.abe.gg_stats.batch.listener;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.annotation.AfterWrite;
import org.springframework.batch.core.annotation.OnProcessError;
import org.springframework.batch.core.annotation.OnReadError;
import org.springframework.batch.core.annotation.OnSkipInProcess;
import org.springframework.batch.core.annotation.OnSkipInRead;
import org.springframework.batch.core.annotation.OnSkipInWrite;
import org.springframework.batch.core.annotation.OnWriteError;
import org.springframework.batch.item.Chunk;

@Slf4j
public class BaseItemExecutionListener<T, S> {

	@OnReadError
	public void onReadError(Exception ex) {
		log.error("Read operation failed.", ex);
	}

	@OnProcessError
	public void onProcessError(T item, Exception ex) {
		log.error("Processing failed for item: {}", item, ex);
	}

	@OnWriteError
	public void onWriteError(Exception exception, Chunk<? extends S> items) {
		log.error("A chunk of {} item(s) failed to write.", items.size(), exception);
	}

	@OnSkipInRead
	public void onSkipInRead(Throwable t) {
		log.warn("An item was skipped during read. Reason: {}", t.getMessage());
	}

	@OnSkipInProcess
	public void onSkipInProcess(T item, Throwable t) {
		log.warn("An item was skipped during process. Item: {}. Reason: {}", item, t.getMessage());
	}

	@OnSkipInWrite
	public void onSkipInWrite(S item, Throwable t) {
		log.warn("An item was skipped during write. Item: {}. Reason: {}", item, t.getMessage());
	}

	@AfterWrite
	public void afterWrite(Chunk<? extends S> items) {
		log.info("Wrote {} item(s) this chunk", items.size());
	}

}
