package com.abe.gg_stats.batch.heroRanking;

import com.abe.gg_stats.entity.HeroRanking;
import com.abe.gg_stats.repository.HeroRankingRepository;
import com.abe.gg_stats.util.LoggingConstants;
import com.abe.gg_stats.util.LoggingUtils;
import com.abe.gg_stats.util.MDCLoggingContext;
import java.util.List;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

/**
 * Writer for HeroRanking entities with improved error handling and logging. Implements
 * proper exception handling and batch processing.
 */
@Component
public class HeroRankingWriter implements ItemWriter<List<HeroRanking>> {

	private final HeroRankingRepository heroRankingRepository;

	public HeroRankingWriter(HeroRankingRepository heroRankingRepository) {
		this.heroRankingRepository = heroRankingRepository;
	}

	private void writeItem(List<? extends HeroRanking> items) {
		// Set up writing context
		String correlationId = MDCLoggingContext.getOrCreateCorrelationId();
		MDCLoggingContext.updateContext("operationType", LoggingConstants.OPERATION_TYPE_BATCH);
		MDCLoggingContext.updateContext("batchType", "herorankings");
		
		if (items.isEmpty()) {
			LoggingUtils.logWarning("No hero ranking items to write", 
				"correlationId=" + correlationId);
			return;
		}
		LoggingUtils.logOperationStart("Writing hero ranking items", 
			"correlationId=" + correlationId,
			"itemsCount=" + items.size());
		heroRankingRepository.saveAll(items);
		LoggingUtils.logOperationSuccess("Successfully saved hero ranking items", 
			"correlationId=" + correlationId,
			"itemsCount=" + items.size());
	}

	@Override
	public void write(Chunk<? extends List<HeroRanking>> chunk) {
		// Set up writing context
		String correlationId = MDCLoggingContext.getOrCreateCorrelationId();
		MDCLoggingContext.updateContext("operationType", LoggingConstants.OPERATION_TYPE_BATCH);
		MDCLoggingContext.updateContext("batchType", "herorankings");
		
		if (chunk.isEmpty()) {
			LoggingUtils.logWarning("Empty chunk received, nothing to write", 
				"correlationId=" + correlationId);
			return;
		}

		LoggingUtils.logOperationStart("Writing hero ranking items to database", 
			"correlationId=" + correlationId,
			"chunkSize=" + chunk.size());

		int successCount = 0;
		int errorCount = 0;
		for (var item : chunk) {
			try {
				writeItem(item);
				successCount++;
				LoggingUtils.logDebug("Successfully wrote hero ranking item", 
					"correlationId=" + correlationId,
					"item=" + item.toString());
			}
			catch (Exception e) {
				errorCount++;
				LoggingUtils.logOperationFailure("hero ranking item write", "Error writing item", e,
					"correlationId=" + correlationId);
			}
		}

		LoggingUtils.logOperationSuccess("Hero ranking write operation completed", 
			"correlationId=" + correlationId,
			"successful=" + successCount,
			"errors=" + errorCount);

		if (errorCount > 0) {
			LoggingUtils.logWarning("Some hero ranking items failed to write", 
				"correlationId=" + correlationId,
				"errorCount=" + errorCount);
		}
	}

}
