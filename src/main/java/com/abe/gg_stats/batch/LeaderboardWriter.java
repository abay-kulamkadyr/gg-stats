package com.abe.gg_stats.batch;

import com.abe.gg_stats.entity.LeaderboardRank;
import com.abe.gg_stats.repository.LeaderboardRankRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class LeaderboardWriter implements ItemWriter<LeaderboardRank> {

	private final LeaderboardRankRepository leaderboardRankRepository;

	@Override
	public void write(Chunk<? extends LeaderboardRank> chunk) throws Exception {
		for (LeaderboardRank leaderboardRank : chunk) {
			try {
				leaderboardRankRepository.save(leaderboardRank);
				log.debug("Saved leaderboard rank: {} (rank: {})", leaderboardRank.getAccountId(),
						leaderboardRank.getRankPosition());
			}
			catch (Exception e) {
				log.error("Error saving leaderboard rank: {}", leaderboardRank.getAccountId(), e);
			}
		}
		log.info("Processed {} leaderboard entries", chunk.size());
	}

}