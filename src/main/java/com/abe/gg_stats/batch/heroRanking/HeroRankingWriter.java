package com.abe.gg_stats.batch.heroRanking;

import com.abe.gg_stats.batch.BaseWriter;
import com.abe.gg_stats.entity.HeroRanking;
import com.abe.gg_stats.repository.HeroRankingRepository;
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
			log.debug("No hero ranking items to write.");
			return;
		}

		log.info("Writing {} hero ranking items.", items.size());

		// Use saveAll() for a single, efficient batch insert/update
		heroRankingRepository.saveAll(items);

		log.debug("Successfully saved {} hero ranking items.", items.size());
	}

	@Override
	public void write(Chunk<? extends List<HeroRanking>> chunk) throws Exception {
		if (chunk.isEmpty()) {
			log.debug("Empty chunk received, nothing to write");
			return;
		}

		log.info("Writing {} items to database", chunk.size());

		int successCount = 0;
		int errorCount = 0;

		for (var item : chunk) {
			try {
				writeItem(item);
				successCount++;
				log.debug("Successfully wrote item: {}", item.toString());
			}
			catch (Exception e) {
				errorCount++;
				log.error("Error writing item: {}", item != null ? item.toString() : "null", e);
			}
		}

		log.info("Write operation completed: {} successful, {} errors", successCount, errorCount);

		if (errorCount > 0) {
			log.warn("Some items failed to write. Check logs for details.");
		}
	}

}
