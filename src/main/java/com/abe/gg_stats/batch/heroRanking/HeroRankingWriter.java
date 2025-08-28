package com.abe.gg_stats.batch.heroRanking;

import com.abe.gg_stats.entity.HeroRanking;
import com.abe.gg_stats.repository.HeroRankingRepository;
import com.abe.gg_stats.util.LoggingUtils;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

/**
 * Writer for HeroRanking entities with improved error handling and logging. Implements
 * proper exception handling and batch processing.
 */
@Component
@Slf4j
public class HeroRankingWriter implements ItemWriter<List<HeroRanking>> {

	private final HeroRankingRepository heroRankingRepository;

	public HeroRankingWriter(HeroRankingRepository heroRankingRepository) {
		this.heroRankingRepository = heroRankingRepository;
	}

	private void writeItem(List<? extends HeroRanking> items) {
		if (items.isEmpty()) {
			LoggingUtils.logWarning("HeroRankingWriter", "No hero ranking items to write.");
			return;
		}
		LoggingUtils.logOperationStart("Writing " + items.size() + " hero ranking items");
		heroRankingRepository.saveAll(items);
		LoggingUtils.logOperationSuccess("Successfully saved " + items.size() + " hero ranking items");
	}

	@Override
	public void write(Chunk<? extends List<HeroRanking>> chunk) {
		if (chunk.isEmpty()) {
			LoggingUtils.logWarning("HeroRanking Writer", "Empty chunk received, nothing to write");
			return;
		}

		LoggingUtils.logOperationStart("HeroRankingWriter", "Writing" + chunk.size() + " items to database");

		int successCount = 0;
		int errorCount = 0;
		for (var item : chunk) {
			try {
				writeItem(item);
				successCount++;
				LoggingUtils.logDebug("Successfully wrote item: {}", () -> item.toString());
			}
			catch (Exception e) {
				errorCount++;
				LoggingUtils.logOperationFailure("hero ranking item write", "Error writing item", e);
			}
		}

		LoggingUtils.logOperationSuccess("HeroRankingWriter", "Write operation completed", "successful=" + successCount,
				"errors=" + errorCount);

		if (errorCount > 0) {
			LoggingUtils.logWarning("HeroRankingWriter", "Some items failed to write. Check logs for details.");
		}
	}

}
