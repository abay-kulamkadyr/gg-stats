package com.abe.gg_stats.batch;

import com.abe.gg_stats.entity.HeroRanking;
import com.abe.gg_stats.repository.HeroRankingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class HeroRankingProcessor implements ItemProcessor<JsonNode, HeroRanking> {

	private final HeroRankingRepository heroRankingRepository;

	@Override
	public HeroRanking process(JsonNode item) throws Exception {
		try {
			if (!item.has("account_id") || !item.has("hero_id")) {
				return null;
			}

			Long accountId = item.get("account_id").asLong();
			Integer heroId = item.get("hero_id").asInt();
			Double score = item.has("score") ? item.get("score").asDouble() : null;

			Optional<HeroRanking> existingRanking = heroRankingRepository.findByHeroIdAndAccountId(accountId, heroId);

			HeroRanking ranking = existingRanking.orElseGet(HeroRanking::new);

			ranking.setAccountId(accountId);
			ranking.setHeroId(heroId);
			ranking.setScore(score);
			log.info("Loaded" + ranking.toString());
			return ranking;
		}
		catch (Exception e) {
			log.error("Error processing hero ranking: {}", item.toString(), e);
			return null;
		}
	}

}
