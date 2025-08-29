package com.abe.gg_stats.batch.team;

import com.abe.gg_stats.batch.BaseWriter;
import com.abe.gg_stats.entity.Team;
import com.abe.gg_stats.repository.TeamRepository;
import org.springframework.stereotype.Component;

@Component
public class TeamWriter extends BaseWriter<Team> {

	private final TeamRepository teamRepository;

	public TeamWriter(TeamRepository teamRepository) {
		this.teamRepository = teamRepository;
	}

	@Override
	protected void writeItem(Team team) {
		teamRepository.save(team);
	}

	@Override
	protected String getItemTypeDescription() {
		return "team";
	}

}