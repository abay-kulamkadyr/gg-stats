package com.abe.gg_stats.batch;

import com.abe.gg_stats.entity.Team;
import com.abe.gg_stats.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class TeamWriter implements ItemWriter<Team> {

	private final TeamRepository teamRepository;

	@Override
	public void write(Chunk<? extends Team> chunk) throws Exception {
		for (Team team : chunk) {
			try {
				teamRepository.save(team);
				log.debug("Saved team: {} ({})", team.getName(), team.getTeamId());
			}
			catch (Exception e) {
				log.error("Error saving team: {}", team.getTeamId(), e);
			}
		}
		log.info("Processed {} teams", chunk.size());
	}

}