package com.abe.gg_stats.batch.hero_ranking;

import com.abe.gg_stats.dto.HeroRankingDto;
import com.abe.gg_stats.dto.mapper.HeroRankingMapper;
import com.abe.gg_stats.entity.HeroRanking;
import com.abe.gg_stats.repository.HeroRankingRepository;
import com.abe.gg_stats.util.LoggingConstants;
import com.abe.gg_stats.util.LoggingUtils;
import com.abe.gg_stats.util.MDCLoggingContext;
import java.util.List;
import java.util.ArrayList;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

/**
 * Writer for HeroRanking items with improved error handling and logging.
 */
@Component
public class HeroRankingWriter implements ItemWriter<List<HeroRankingDto>> {

	private final HeroRankingRepository heroRankingRepository;

	private final HeroRankingMapper heroRankingMapper;

	public HeroRankingWriter(HeroRankingRepository heroRankingRepository, HeroRankingMapper heroRankingMapper) {
		this.heroRankingRepository = heroRankingRepository;
		this.heroRankingMapper = heroRankingMapper;
	}

	private void writeItem(List<? extends HeroRankingDto> items) {
		// Set up writing context
		String correlationId = MDCLoggingContext.getOrCreateCorrelationId();
		MDCLoggingContext.updateContext("operationType", LoggingConstants.OPERATION_TYPE_BATCH);
		MDCLoggingContext.updateContext("batchType", "herorankings");

		if (items.isEmpty()) {
			LoggingUtils.logWarning("No hero ranking items to write", "correlationId=" + correlationId);
			return;
		}
		LoggingUtils.logOperationStart("Writing hero ranking items", "correlationId=" + correlationId,
				"itemsCount=" + items.size());
		List<HeroRankingDto> copy = new ArrayList<>(items);
		List<HeroRanking> entities = heroRankingMapper.dtoToEntity(copy);
		heroRankingRepository.saveAll(entities);
		LoggingUtils.logOperationSuccess("Successfully saved hero ranking items", "correlationId=" + correlationId,
				"itemsCount=" + items.size());
	}

	@Override
	public void write(Chunk<? extends List<HeroRankingDto>> chunk) {
		// Set up writing context
		String correlationId = MDCLoggingContext.getOrCreateCorrelationId();
		MDCLoggingContext.updateContext("operationType", LoggingConstants.OPERATION_TYPE_BATCH);
		MDCLoggingContext.updateContext("batchType", "herorankings");

		if (chunk.isEmpty()) {
			LoggingUtils.logWarning("Empty chunk received, nothing to write", "correlationId=" + correlationId);
			return;
		}

		LoggingUtils.logOperationStart("Writing hero ranking items to database", "correlationId=" + correlationId,
				"chunkSize=" + chunk.size());

		int successCount = 0;
		int errorCount = 0;
		for (var item : chunk) {
			try {
				writeItem(item);
				successCount++;
				LoggingUtils.logDebug("Successfully wrote hero ranking item", "correlationId=" + correlationId,
						"item=" + item);
			}
			catch (Exception e) {
				errorCount++;
				LoggingUtils.logOperationFailure("hero ranking item write", "Error writing item", e,
						"correlationId=" + correlationId);
			}
		}

		LoggingUtils.logOperationSuccess("Hero ranking write operation completed", "correlationId=" + correlationId,
				"successful=" + successCount, "errors=" + errorCount);

		if (errorCount > 0) {
			LoggingUtils.logWarning("Some hero ranking items failed to write", "correlationId=" + correlationId,
					"errorCount=" + errorCount);
		}
	}

}
