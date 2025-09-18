package com.abe.gg_stats.batch.listener;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.concurrent.atomic.AtomicLong;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.ItemReadListener;
import org.springframework.batch.core.ItemWriteListener;
import org.springframework.batch.core.SkipListener;
import org.springframework.batch.core.listener.ItemListenerSupport;
import org.springframework.batch.item.Chunk;

@Slf4j
public class MatchesItemListeners extends ItemListenerSupport<JsonNode, JsonNode>
		implements ItemReadListener<JsonNode>, ItemWriteListener<JsonNode>, SkipListener<JsonNode, JsonNode> {

	private final AtomicLong totalWritten = new AtomicLong(0);

	@Override
	public void onWriteError(Exception exception, Chunk<? extends JsonNode> items) {
		log.warn("Write error size={}", items.size(), exception);
	}

	@Override
	public void afterWrite(Chunk<? extends JsonNode> items) {
		long chunk = items.size();
		long total = totalWritten.addAndGet(chunk);
		log.info("Wrote {} item(s) this chunk; total written so far={}", chunk, total);
	}

	@Override
	public void onReadError(Exception ex) {
		log.warn("Read error", ex);
	}

	@Override
	public void onSkipInRead(Throwable t) {
		log.info("Skip in read reason={}", t.toString());
	}

	@Override
	public void onSkipInProcess(JsonNode item, Throwable t) {
		log.info("Skip in process item={} reason={}", item, t.toString());
	}

	@Override
	public void onSkipInWrite(JsonNode item, Throwable t) {
		log.info("Skip in write item={} reason={}", item, t.toString());
	}

}