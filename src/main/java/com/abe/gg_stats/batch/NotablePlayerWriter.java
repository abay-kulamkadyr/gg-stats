package com.abe.gg_stats.batch;

import com.abe.gg_stats.entity.NotablePlayer;
import com.abe.gg_stats.repository.NotablePlayerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotablePlayerWriter implements ItemWriter<NotablePlayer> {

	private final NotablePlayerRepository notablePlayerRepository;

	@Override
	public void write(Chunk<? extends NotablePlayer> chunk) throws Exception {
		for (NotablePlayer notablePlayer : chunk) {
			try {
				notablePlayerRepository.save(notablePlayer);
				log.debug("Saved pro player: {} ({})", notablePlayer.getName(), notablePlayer.getAccountId());
			}
			catch (Exception e) {
				log.error("Error saving pro player: {}", notablePlayer.getAccountId(), e);
			}
		}
		log.info("Processed {} pro players", chunk.size());
	}

}