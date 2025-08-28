package com.abe.gg_stats.batch.heroRanking;

import com.abe.gg_stats.batch.BaseApiReader;
import com.abe.gg_stats.config.BatchExpirationConfig;
import com.abe.gg_stats.repository.HeroRankingRepository;
import com.abe.gg_stats.repository.HeroRepository;
import com.abe.gg_stats.service.OpenDotaApiService;
import com.abe.gg_stats.util.LoggingUtils;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Reader for HeroRanking entities with improved error handling and performance. Fetches
 * hero rankings from the OpenDota API and provides them for processing.
 */
@Component
@Slf4j
public class HeroRankingReader extends BaseApiReader<JsonNode> {

	private final HeroRepository heroRepository;

	private final HeroRankingRepository heroRankingRepository;

	@Autowired
	public HeroRankingReader(OpenDotaApiService openDotaApiService, HeroRepository heroRepository,
			BatchExpirationConfig expirationConfig, HeroRankingRepository heroRankingRepository) {
		super(openDotaApiService, expirationConfig);
		this.heroRepository = heroRepository;
		this.heroRankingRepository = heroRankingRepository;
	}

	@Override
	protected void initialize() {
		List<Integer> heroIds = heroRepository.findAllIds();
		List<JsonNode> heroRankings = new ArrayList<>();
		heroIds.forEach(heroId -> fetchDataFromApiIfNeeded(heroId).ifPresent(heroRankings::add));
		LoggingUtils
			.logOperationSuccess("Initialized hero ranking reader with " + heroRankings.size() + "heroes loaded");
		this.dataIterator = heroRankings.iterator();
	}

	Optional<JsonNode> fetchDataFromApiIfNeeded(Integer heroId) {

		LoggingUtils.logMethodEntry("Updating hero ranking info for hero_id " + heroId);
		Optional<LocalDateTime> latestUpdate = heroRankingRepository.findMaxUpdatedAt();
		if (latestUpdate.isPresent() && super.noRefreshNeeded(latestUpdate.get())) {
			LoggingUtils.logWarning("Heroes data is up to date (last update: " + latestUpdate + ")");
			return Optional.empty();
		}

		// Fetch player info from OpenDota API
		return openDotaApiService.getHeroRanking(heroId);
	}

	@Override
	protected String getExpirationConfigName() {
		return "herorankings";
	}

}