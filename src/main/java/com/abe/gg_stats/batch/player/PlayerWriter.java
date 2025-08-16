package com.abe.gg_stats.batch.player;

import com.abe.gg_stats.batch.BaseWriter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class PlayerWriter extends BaseWriter<Long> {

	public PlayerWriter() {
		// No dependencies needed since data is persisted in the processor
	}

	@Override
	protected void writeItem(Long accountId) throws Exception {
		// Data is already persisted by PlayerUpdateService in the processor
		// This writer just logs completion for consistency with other batch jobs
		log.debug("Player data for account_id {} was already persisted in processor", accountId);
	}

	@Override
	protected String getItemTypeDescription() {
		return "player account ID (already persisted)";
	}

}
