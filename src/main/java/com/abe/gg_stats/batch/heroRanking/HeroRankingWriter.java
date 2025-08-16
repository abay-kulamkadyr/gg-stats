package com.abe.gg_stats.batch.heroRanking;

import com.abe.gg_stats.batch.BaseWriter;
import com.abe.gg_stats.entity.HeroRanking;
import com.abe.gg_stats.repository.HeroRankingRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Writer for HeroRanking entities with improved error handling and logging. Implements
 * proper exception handling and batch processing.
 */
@Component
@Slf4j
public class HeroRankingWriter extends BaseWriter<HeroRanking> {

	private final HeroRankingRepository heroRankingRepository;

	public HeroRankingWriter(HeroRankingRepository heroRankingRepository) {
		this.heroRankingRepository = heroRankingRepository;
	}

	@Override
	protected void writeItem(HeroRanking ranking) throws Exception {
		if (ranking == null) {
			log.warn("Received null ranking, skipping");
			return;
		}

		heroRankingRepository.save(ranking);
		log.debug("Saved hero ranking: hero_id={}, account_id={}, score={}", ranking.getHeroId(),
				ranking.getAccountId(), ranking.getScore());
	}

	@Override
	protected String getItemTypeDescription() {
		return "hero ranking";
	}

}
