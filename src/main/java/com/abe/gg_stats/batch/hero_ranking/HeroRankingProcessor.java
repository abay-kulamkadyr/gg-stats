package com.abe.gg_stats.batch.hero_ranking;

import com.abe.gg_stats.config.batch.BatchExpirationConfig;
import com.abe.gg_stats.dto.request.opendota.OpenDotaHeroRankingDto;
import com.abe.gg_stats.repository.HeroRankingRepository;
import com.abe.gg_stats.service.OpenDotaApiService;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Component
public class HeroRankingProcessor implements ItemProcessor<Integer, List<OpenDotaHeroRankingDto>> {

	private final OpenDotaApiService openDotaApiService;

	private final HeroRankingRepository heroRankingRepository;

	private final BatchExpirationConfig batchExpirationConfig;

	@Autowired
	public HeroRankingProcessor(OpenDotaApiService openDotaApiService, HeroRankingRepository heroRankingRepository,
			BatchExpirationConfig batchExpirationConfig) {
		this.openDotaApiService = openDotaApiService;
		this.heroRankingRepository = heroRankingRepository;
		this.batchExpirationConfig = batchExpirationConfig;
	}

	@Override
	public List<OpenDotaHeroRankingDto> process(@NonNull Integer heroId) throws Exception {
		Optional<Instant> latestUpdate = heroRankingRepository.findLastUpdateByHeroId(heroId);

		// Check if a refresh is needed based on the expiration config
		if (latestUpdate.isPresent() && !isRefreshNeeded(latestUpdate.get())) {
			return null;
		}

		Optional<JsonNode> apiData = openDotaApiService.getHeroRanking(heroId);

		// If API data is present, convert it to a DTO list
		return apiData.map(this::mapJsonToDtoList).orElse(null);
	}

	private boolean isRefreshNeeded(Instant lastUpdate) {
		Instant expirationTime = lastUpdate.plus(batchExpirationConfig.getDurationByConfigName("herorankings"));
		return Instant.now().isAfter(expirationTime);
	}

	private List<OpenDotaHeroRankingDto> mapJsonToDtoList(JsonNode rootNode) {
		Integer heroId = rootNode.get("hero_id").asInt();
		JsonNode rankingsNode = rootNode.get("rankings");

		return StreamSupport.stream(rankingsNode.spliterator(), false)
			.map(rankingNode -> new OpenDotaHeroRankingDto(rankingNode.get("account_id").asLong(), heroId,
					rankingNode.get("score").asDouble()))
			.collect(Collectors.toList());
	}

}