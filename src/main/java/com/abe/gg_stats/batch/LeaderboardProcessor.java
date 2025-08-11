package com.abe.gg_stats.batch;

import com.abe.gg_stats.entity.LeaderboardRank;
import com.abe.gg_stats.entity.Player;
import com.abe.gg_stats.repository.PlayerRepository;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class LeaderboardProcessor implements ItemProcessor<JsonNode, LeaderboardRank> {

	private final PlayerRepository playerRepository;

	@Override
	public LeaderboardRank process(JsonNode item) throws Exception {
		try {
			if (!item.has("account_id") || item.get("account_id").isNull()) {
				return null;
			}

			Long accountId = item.get("account_id").asLong();

			LeaderboardRank leaderboardRank = new LeaderboardRank();
			leaderboardRank.setAccountId(accountId);

			// Find or create player entity
			Optional<Player> playerOpt = playerRepository.findById(accountId);
			if (playerOpt.isEmpty()) {
				Player player = new Player();
				player.setAccountId(accountId);
				player = playerRepository.save(player);
				leaderboardRank.setPlayer(player);
			}
			else {
				leaderboardRank.setPlayer(playerOpt.get());
			}

			leaderboardRank.setRankPosition(item.has("rank") ? item.get("rank").asInt() : null);
			leaderboardRank
				.setRating(item.has("solo_competitive_rank") ? item.get("solo_competitive_rank").asInt() : null);

			return leaderboardRank;
		}
		catch (Exception e) {
			log.error("Error processing leaderboard entry: {}", item.toString(), e);
			return null;
		}
	}

}
