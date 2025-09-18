package com.abe.gg_stats.batch.player;

import com.abe.gg_stats.repository.HeroRankingRepository;
import com.abe.gg_stats.repository.NotablePlayerRepository;
import com.abe.gg_stats.repository.PlayerRepository;
import java.util.Iterator;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.batch.item.ItemReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reads a list of unique account IDs that require updating, from various data sources.
 * This component acts as a key provider for the batch job.
 */
@Component
public class PlayerReader implements ItemReader<Long> {

	private final HeroRankingRepository heroRankingRepository;

	private final NotablePlayerRepository notablePlayerRepository;

	private final PlayerRepository playerRepository;

	private Iterator<Long> accountIdIterator;

	@Autowired
	public PlayerReader(HeroRankingRepository heroRankingRepository, NotablePlayerRepository notablePlayerRepository,
			PlayerRepository playerRepository) {
		this.heroRankingRepository = heroRankingRepository;
		this.notablePlayerRepository = notablePlayerRepository;
		this.playerRepository = playerRepository;
	}

	@Override
	@Transactional(readOnly = true)
	public Long read() {
		if (accountIdIterator == null) {
			this.accountIdIterator = collectUniqueAccountIds().iterator();
		}

		if (accountIdIterator.hasNext()) {
			return accountIdIterator.next();
		}

		return null;
	}

	private Set<Long> collectUniqueAccountIds() {
		// Collect all unique account IDs directly from the repositories.
		Set<Long> heroRankingIds = heroRankingRepository.findAllIds();
		Set<Long> notablePlayerIds = notablePlayerRepository.findAllIds();
		Set<Long> allPlayersIds = playerRepository.findAllIds();

		// Combine the sets into a single stream to ensure a unique list of IDs.
		return Stream.of(heroRankingIds, notablePlayerIds, allPlayersIds)
			.flatMap(Set::stream)
			.collect(Collectors.toSet());
	}

}