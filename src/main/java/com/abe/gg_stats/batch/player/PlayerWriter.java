package com.abe.gg_stats.batch.player;

import com.abe.gg_stats.batch.BaseWriter;
import com.abe.gg_stats.entity.Player;
import com.abe.gg_stats.repository.PlayerRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class PlayerWriter extends BaseWriter<Player> {

	private final PlayerRepository playerRepository;

	@Autowired
	public PlayerWriter(PlayerRepository playerRepository) {
		this.playerRepository = playerRepository;
	}

	@Override
	protected void writeItem(Player player) {
		// Data is already persisted by PlayerUpdateService in the processor
		// This writer just logs completion for consistency with other batch jobs
		playerRepository.save(player);
		log.debug("Player data for account_id {} has been saved", player.getAccountId());
	}

	@Override
	protected String getItemTypeDescription() {
		return "player account ID (already persisted)";
	}

}
