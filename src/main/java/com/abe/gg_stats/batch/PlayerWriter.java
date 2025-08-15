package com.abe.gg_stats.batch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class PlayerWriter implements ItemWriter<Long> {

	@Override
	public void write(Chunk<? extends Long> chunk) throws Exception {
		log.info("Processed {} account_ids in this chunk", chunk.size());
	}

}
