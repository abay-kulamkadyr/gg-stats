package com.abe.gg_stats.batch;

import com.abe.gg_stats.entity.HeroRanking;
import com.abe.gg_stats.repository.HeroRankingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

/**
 * Writer for HeroRanking entities with improved error handling and logging. Implements
 * proper exception handling and batch processing.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class HeroRankingWriter implements ItemWriter<HeroRanking> {

	private final HeroRankingRepository heroRankingRepository;

	@Override
	public void write(Chunk<? extends HeroRanking> chunk) throws Exception {
		if (chunk == null || chunk.isEmpty()) {
			log.debug("Received empty chunk, nothing to write");
			return;
		}

		int successCount = 0;
		int errorCount = 0;

		for (HeroRanking ranking : chunk) {
			try {
				if (ranking == null) {
					log.warn("Received null ranking in chunk, skipping");
					continue;
				}

				heroRankingRepository.save(ranking);
				successCount++;

				log.debug("Saved hero ranking: hero_id={}, account_id={}, score={}", ranking.getHeroId(),
						ranking.getAccountId(), ranking.getScore());
			}
			catch (Exception e) {
				errorCount++;
				log.error("Error saving hero ranking: hero_id={}, account_id={}",
						ranking != null ? ranking.getHeroId() : "null",
						ranking != null ? ranking.getAccountId() : "null", e);

				// Re-throw to trigger retry/skip logic
				throw new HeroRankingWriteException("Failed to write hero ranking", e);
			}
		}

		log.info("Processed {} hero rankings: {} successful, {} errors", chunk.size(), successCount, errorCount);
	}

	/**
	 * Custom exception for hero ranking write errors
	 */
	public static class HeroRankingWriteException extends Exception {

		public HeroRankingWriteException(String message) {
			super(message);
		}

		public HeroRankingWriteException(String message, Throwable cause) {
			super(message, cause);
		}

	}

}
