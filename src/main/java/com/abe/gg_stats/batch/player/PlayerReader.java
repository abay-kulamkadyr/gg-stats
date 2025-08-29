package com.abe.gg_stats.batch.player;

import com.abe.gg_stats.batch.BaseApiReader;
import com.abe.gg_stats.config.BatchExpirationConfig;
import com.abe.gg_stats.entity.HeroRanking;
import com.abe.gg_stats.entity.NotablePlayer;
import com.abe.gg_stats.entity.Player;
import com.abe.gg_stats.repository.HeroRankingRepository;
import com.abe.gg_stats.repository.NotablePlayerRepository;
import com.abe.gg_stats.repository.PlayerRepository;
import com.abe.gg_stats.service.OpenDotaApiService;
import com.abe.gg_stats.util.LoggingConstants;
import com.abe.gg_stats.util.LoggingUtils;
import com.abe.gg_stats.util.MDCLoggingContext;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Chunked Player Reader that fetches player data in smaller batches to avoid memory
 * issues and API rate limiting.
 */
@Component
public class PlayerReader extends BaseApiReader<JsonNode> {

	private final HeroRankingRepository heroRankingRepository;

	private final NotablePlayerRepository notablePlayerRepository;

	private final PlayerRepository playerRepository;

	@Value("${app.batch.players.chunk-size:60}")
	private int chunkSize;

	private Iterator<Long> accountIdIterator;

	private List<JsonNode> currentChunk;

	private int currentChunkIndex = 0;

	private int totalProcessed = 0;

	private int totalAccounts = 0;

	@Autowired
	public PlayerReader(OpenDotaApiService openDotaApiService, BatchExpirationConfig batchExpirationConfig,
			HeroRankingRepository heroRankingRepository, NotablePlayerRepository notablePlayerRepository,
			PlayerRepository playerRepository) {
		super(openDotaApiService, batchExpirationConfig);
		this.heroRankingRepository = heroRankingRepository;
		this.notablePlayerRepository = notablePlayerRepository;
		this.playerRepository = playerRepository;
	}

	@Override
	protected void initialize() {
		// Set up reader context
		String correlationId = MDCLoggingContext.getOrCreateCorrelationId();
		MDCLoggingContext.updateContext("operationType", LoggingConstants.OPERATION_TYPE_BATCH);
		MDCLoggingContext.updateContext("batchType", "players");
		
		LoggingUtils.logOperationStart("Initializing chunked player reader", 
			"correlationId=" + correlationId);
		
		Set<Long> allAccountIds = collectAllAccountIds();
		totalAccounts = allAccountIds.size();

		// Convert to list and create iterator
		List<Long> accountIdsList = new ArrayList<>(allAccountIds);
		accountIdIterator = accountIdsList.iterator();

		// Initialize first chunk
		currentChunk = new ArrayList<>();
		currentChunkIndex = 0;
		totalProcessed = 0;

		LoggingUtils.logOperationStart("chunked player reader initialization", 
			"correlationId=" + correlationId,
			"totalAccounts=" + totalAccounts,
			"chunkSize=" + chunkSize);

		// Pre-fetch first chunk
		fetchNextChunk();
	}

	@Override
	public JsonNode read() {
		if (!initialized) {
			initialize();
			initialized = true;
		}

		// If current chunk is empty, try to fetch next chunk
		if (currentChunk.isEmpty()) {
			if (!fetchNextChunk()) {
				return null; // No more data
			}
		}

		// Return next item from current chunk
		if (!currentChunk.isEmpty()) {
			JsonNode item = currentChunk.removeFirst();
			totalProcessed++;

			// Log progress every 100 items
			if (totalProcessed % 100 == 0 || totalProcessed == totalAccounts) {
				LoggingUtils.logBatchProgress("player processing", totalProcessed, totalAccounts);
			}

			return item;
		}

		return null;
	}

	/**
	 * Fetch the next chunk of player data
	 */
	private boolean fetchNextChunk() {
		if (!accountIdIterator.hasNext()) {
			LoggingUtils.logOperationSuccess("chunked player reader", "totalProcessed=" + totalProcessed,
					"totalAccounts=" + totalAccounts);
			return false;
		}

		LoggingUtils.logOperationStart("fetching player chunk", "chunkIndex=" + (currentChunkIndex + 1),
				"processed=" + totalProcessed);

		currentChunk.clear();
		int chunkCount = 0;
		long startTime = System.currentTimeMillis();

		// Process accounts in current chunk
		while (accountIdIterator.hasNext() && chunkCount < chunkSize) {
			Long accountId = accountIdIterator.next();

			try {
				Optional<JsonNode> playerData = fetchDataFromApi(accountId);
				if (playerData.isPresent()) {
					currentChunk.add(playerData.get());
					chunkCount++;
				}

			}
			catch (Exception e) {
				LoggingUtils.logOperationFailure("player data fetch", "Failed to fetch data for account " + accountId,
						e);
			}
		}

		currentChunkIndex++;
		long chunkTime = System.currentTimeMillis() - startTime;

		LoggingUtils.logOperationSuccess("player chunk fetched", "chunkIndex=" + currentChunkIndex,
				"items=" + chunkCount, "time=" + chunkTime + "ms");

		return !currentChunk.isEmpty();
	}

	/**
	 * Collect all unique account IDs from various sources
	 */
	private Set<Long> collectAllAccountIds() {
		Set<Long> accountIds = new HashSet<>();
		Set<Long> filteredAccountIds = new HashSet<>();
		try {
			// Collect from HeroRanking (most recent data)
			List<HeroRanking> heroRankings = heroRankingRepository.findAll();
			heroRankings.stream()
				.filter(ranking -> ranking != null && ranking.getAccountId() != null)
				.forEach(ranking -> accountIds.add(ranking.getAccountId()));

			LoggingUtils.logDebug("Collected account IDs from HeroRanking", "count=" + heroRankings.size());

			// Collect from NotablePlayer
			List<NotablePlayer> notablePlayers = notablePlayerRepository.findAll();
			notablePlayers.stream()
				.filter(player -> player != null && player.getAccountId() != null)
				.forEach(player -> accountIds.add(player.getAccountId()));

			LoggingUtils.logDebug("Collected account IDs from NotablePlayer", "count=" + notablePlayers.size());

			accountIds.stream()
				.map(playerRepository::findById)
				.filter(Optional::isPresent)
				.map(Optional::get)
				.filter(player -> !super.noRefreshNeeded(player.getUpdatedAt()) || player.getSteamId() == null)
				.map(Player::getAccountId)
				.forEach(filteredAccountIds::add);

			// Collect from existing Players
			List<Player> players = playerRepository.findAll();
			players.stream()
				.filter(player -> player != null && player.getAccountId() != null
						&& (!super.noRefreshNeeded(player.getUpdatedAt()) || player.getSteamId() == null))
				.forEach(player -> filteredAccountIds.add(player.getAccountId()));

			LoggingUtils.logDebug("Collected account IDs from Player", "count=" + players.size());

		}
		catch (Exception e) {
			LoggingUtils.logOperationFailure("account ID collection", "Failed to collect account IDs", e);
		}

		LoggingUtils.logOperationSuccess("account ID collection", "totalUnique=" + filteredAccountIds.size());
		return filteredAccountIds;
	}

	@Transactional
	public Optional<JsonNode> fetchDataFromApi(Long accountId) {
		try {

			// Fetch player info from OpenDota API
			Optional<JsonNode> playerData = openDotaApiService.getPlayer(accountId);
			if (playerData.isEmpty()) {
				LoggingUtils.logWarning("No data from OpenDota API", "accountId=" + accountId);
				return Optional.empty();
			}

			return playerData;

		}
		catch (Exception e) {
			LoggingUtils.logOperationFailure("player data fetch", "Failed to fetch data for account " + accountId, e);
			return Optional.empty();
		}
	}

	@Override
	protected String getExpirationConfigName() {
		return "players";
	}

	/**
	 * Get current processing statistics
	 */
	public Map<String, Object> getProcessingStats() {
		Map<String, Object> stats = new HashMap<>();
		stats.put("totalAccounts", totalAccounts);
		stats.put("totalProcessed", totalProcessed);
		stats.put("currentChunkIndex", currentChunkIndex);
		stats.put("currentChunkSize", currentChunk.size());
		stats.put("chunkSize", chunkSize);
		stats.put("progressPercentage", totalAccounts > 0 ? (totalProcessed * 100.0 / totalAccounts) : 0.0);
		return stats;
	}

}
