package com.abe.gg_stats.batch;

import com.abe.gg_stats.entity.ProPlayer;
import com.abe.gg_stats.repository.ProPlayerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProPlayerWriter implements ItemWriter<ProPlayer> {

	private final ProPlayerRepository proPlayerRepository;

	@Override
	public void write(Chunk<? extends ProPlayer> chunk) throws Exception {
		for (ProPlayer proPlayer : chunk) {
			try {
				proPlayerRepository.save(proPlayer);
				log.debug("Saved pro player: {} ({})", proPlayer.getName(), proPlayer.getAccountId());
			}
			catch (Exception e) {
				log.error("Error saving pro player: {}", proPlayer.getAccountId(), e);
			}
		}
		log.info("Processed {} pro players", chunk.size());
	}

}