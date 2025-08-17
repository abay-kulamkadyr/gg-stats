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
import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Slf4j
public class PlayerReader extends BaseApiReader<JsonNode> {

	private final HeroRankingRepository heroRankingRepository;

	private final NotablePlayerRepository notablePlayerRepository;

	private final PlayerRepository playerRepository;

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
		Set<Long> accountIds = collectAllAccountIds();
		List<JsonNode> playersData = new ArrayList<>();
		accountIds.forEach(accountId -> fetchDataFromAPIifNeeded(accountId).ifPresent(playersData::add));

		this.dataIterator = playersData.iterator();
		log.info("Initialized player reader with {} players loaded", playersData.size());
	}

	/**
	 * Collects all unique account IDs from various sources
	 */
	private Set<Long> collectAllAccountIds() {
		Set<Long> accountIds = new HashSet<>();

		List<HeroRanking> heroRankings = heroRankingRepository.findAll();
		heroRankings.forEach(ranking -> {
			if (ranking != null && ranking.getAccountId() != null) {
				accountIds.add(ranking.getAccountId());
			}
		});
		log.info("Collected {} account_ids from HeroRanking", heroRankings.size());

		// Collect from NotablePlayer
		List<NotablePlayer> notablePlayers = notablePlayerRepository.findAll();
		notablePlayers.forEach(player -> {
			if (player != null && player.getAccountId() != null) {
				accountIds.add(player.getAccountId());
			}
		});
		log.info("Collected {} account_ids from NotablePlayer", notablePlayers.size());

		// Collect from existing Players (in case we need to update them)
		List<Player> players = playerRepository.findAll();
		players.forEach(player -> {
			if (player != null && player.getAccountId() != null) {
				accountIds.add(player.getAccountId());
			}
		});
		log.info("Collected {} account_ids from Player", players.size());

		log.info("Total unique account_ids collected: {}", accountIds.size());

		return accountIds;
	}

	@Override
	protected String getExpirationConfigName() {
		return "players";
	}

	@Transactional
	public Optional<JsonNode> fetchDataFromAPIifNeeded(Long accountId) {
		log.info("Updating player info for account_id: {}", accountId);

		Optional<Player> existingPlayerOpt = playerRepository.findById(accountId);

		if (existingPlayerOpt.isPresent() && super.noRefreshNeeded(existingPlayerOpt.get().getUpdatedAt())) {
			log.info("Player {} data is accurate, skipping ", accountId);
			return Optional.empty();
		}

		// Fetch player info from OpenDota API
		Optional<JsonNode> playerData = openDotaApiService.getPlayer(accountId);
		if (playerData.isEmpty()) {
			log.warn("No data received from OpenDota API for account_id: {}", accountId);
			return Optional.empty();
		}

		return playerData;
	}

}
