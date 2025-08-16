package com.abe.gg_stats.batch.player;

import com.abe.gg_stats.batch.BaseDatabaseReader;
import com.abe.gg_stats.entity.HeroRanking;
import com.abe.gg_stats.entity.NotablePlayer;
import com.abe.gg_stats.entity.Player;
import com.abe.gg_stats.repository.HeroRankingRepository;
import com.abe.gg_stats.repository.NotablePlayerRepository;
import com.abe.gg_stats.repository.PlayerRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
@Slf4j
public class PlayerReader extends BaseDatabaseReader<Long> {

	private final HeroRankingRepository heroRankingRepository;

	private final NotablePlayerRepository notablePlayerRepository;

	private final PlayerRepository playerRepository;

	public PlayerReader(HeroRankingRepository heroRankingRepository, NotablePlayerRepository notablePlayerRepository,
			PlayerRepository playerRepository) {
		this.heroRankingRepository = heroRankingRepository;
		this.notablePlayerRepository = notablePlayerRepository;
		this.playerRepository = playerRepository;
	}

	@Override
	protected void initialize() {
		Set<Long> accountIds = collectAllAccountIds();
		this.dataIterator = accountIds.iterator();
		log.info("Initialized player reader with {} unique account_ids", accountIds.size());
	}

	/**
	 * Collects all unique account IDs from various sources
	 */
	private Set<Long> collectAllAccountIds() {
		Set<Long> accountIds = new HashSet<>();

		try {
			// Collect from HeroRanking
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
		}
		catch (Exception e) {
			log.error("Error collecting account IDs", e);
			throw new PlayerReadException("Failed to collect account IDs", e);
		}

		return accountIds;
	}

	@Override
	protected String getDataTypeDescription() {
		return "player account IDs";
	}

	/**
	 * Custom exception for player read errors
	 */
	public static class PlayerReadException extends RuntimeException {

		public PlayerReadException(String message) {
			super(message);
		}

		public PlayerReadException(String message, Throwable cause) {
			super(message, cause);
		}

	}

}
