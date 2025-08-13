package com.abe.gg_stats.batch;

import com.abe.gg_stats.entity.HeroRanking;
import com.abe.gg_stats.repository.HeroRankingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class HeroRankingWriter implements ItemWriter<HeroRanking> {

	private final HeroRankingRepository heroRankingRepository;

	@Override
	public void write(Chunk<? extends HeroRanking> chunk) throws Exception {
		for (HeroRanking ranking : chunk) {
			try {
				log.info("trying to save " + ranking.toString());
				heroRankingRepository.save(ranking);
				log.debug("Saved hero ranking: hero_id={}, account_id={}, score={}", ranking.getHeroId(),
						ranking.getAccountId(), ranking.getScore());
			}
			catch (Exception e) {
				log.error("Error saving hero ranking: hero_id={}, account_id={}", ranking.getHeroId(),
						ranking.getAccountId(), e);
			}
		}
		log.info("Processed {} hero rankings", chunk.size());
	}

}
