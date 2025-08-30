package com.abe.gg_stats.batch.hero_ranking;

import com.abe.gg_stats.batch.BaseApiReader;
import com.abe.gg_stats.config.BatchExpirationConfig;
import com.abe.gg_stats.repository.HeroRankingRepository;
import com.abe.gg_stats.repository.HeroRepository;
import com.abe.gg_stats.service.OpenDotaApiService;
import com.abe.gg_stats.util.LoggingConstants;
import com.abe.gg_stats.util.LoggingUtils;
import com.abe.gg_stats.util.MDCLoggingContext;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Reader for HeroRanking entities with improved error handling and performance. Fetches
 * hero rankings from the OpenDota API and provides them for processing.
 */
@Component
public class HeroRankingReader extends BaseApiReader {

	private final HeroRepository heroRepository;

	private final HeroRankingRepository heroRankingRepository;

	public HeroRankingReader(OpenDotaApiService openDotaApiService, HeroRepository heroRepository,
			BatchExpirationConfig expirationConfig, HeroRankingRepository heroRankingRepository) {
		super(openDotaApiService, expirationConfig);
		this.heroRepository = heroRepository;
		this.heroRankingRepository = heroRankingRepository;
	}

	@Override
	protected void initialize() {
		// Set up reader context
		String correlationId = MDCLoggingContext.getOrCreateCorrelationId();
		MDCLoggingContext.updateContext("operationType", LoggingConstants.OPERATION_TYPE_BATCH);
		MDCLoggingContext.updateContext("batchType", "herorankings");

		LoggingUtils.logOperationStart("Initializing hero ranking reader", "correlationId=" + correlationId);

		List<Integer> heroIds = heroRepository.findAllIds();
		List<JsonNode> heroRankings = new ArrayList<>();
		heroIds.forEach(heroId -> fetchDataFromApiIfNeeded(heroId).ifPresent(heroRankings::add));

		LoggingUtils.logOperationSuccess("Initialized hero ranking reader", "correlationId=" + correlationId,
				"heroesLoaded=" + heroRankings.size());
		this.dataIterator = heroRankings.iterator();
	}

	Optional<JsonNode> fetchDataFromApiIfNeeded(Integer heroId) {
		// Set up processing context
		String correlationId = MDCLoggingContext.getOrCreateCorrelationId();
		MDCLoggingContext.updateContext("operationType", LoggingConstants.OPERATION_TYPE_BATCH);
		MDCLoggingContext.updateContext("batchType", "herorankings");

		LoggingUtils.logMethodEntry("Updating hero ranking info for hero_id", () -> "correlationId=" + correlationId,
				() -> "heroId=" + heroId);

		Optional<LocalDateTime> latestUpdate = heroRankingRepository.findMaxUpdatedAt();
		if (latestUpdate.isPresent() && super.noRefreshNeeded(latestUpdate.get())) {
			LoggingUtils.logWarning("Hero ranking data is up to date", "correlationId=" + correlationId,
					"lastUpdate=" + latestUpdate.get());
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