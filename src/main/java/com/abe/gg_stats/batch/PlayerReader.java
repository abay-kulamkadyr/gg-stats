package com.abe.gg_stats.batch;

import com.abe.gg_stats.entity.HeroRanking;
import com.abe.gg_stats.entity.NotablePlayer;
import com.abe.gg_stats.entity.Player;
import com.abe.gg_stats.repository.HeroRankingRepository;
import com.abe.gg_stats.repository.NotablePlayerRepository;
import com.abe.gg_stats.repository.PlayerRepository;
import com.abe.gg_stats.service.OpenDotaApiService;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemReader;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class PlayerReader implements ItemReader<Long> {

	private final HeroRankingRepository heroRankingRepository;

	private final NotablePlayerRepository notablePlayerRepository;

	private final PlayerRepository playerRepository;

	private final OpenDotaApiService openDotaApiService;

	private Iterator<Long> accountIdIterator;

	private boolean initialized = false;

	@Override
	public Long read() {
		if (!initialized || accountIdIterator == null) {
			Set<Long> accountIds = collectAllAccountIds();
			accountIdIterator = accountIds.iterator();
			log.info("Initialized account info reader with {} account_ids", accountIds.size());
			initialized = true;
		}

		if (accountIdIterator.hasNext()) {
			return accountIdIterator.next();
		}

		return null; // End of data
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
