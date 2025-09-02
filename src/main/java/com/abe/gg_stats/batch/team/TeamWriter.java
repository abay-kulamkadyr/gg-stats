package com.abe.gg_stats.batch.team;

import com.abe.gg_stats.batch.BaseWriter;
import com.abe.gg_stats.dto.TeamDto;
import com.abe.gg_stats.dto.mapper.TeamMapper;
import com.abe.gg_stats.entity.Team;
import com.abe.gg_stats.repository.TeamRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class TeamWriter extends BaseWriter<TeamDto> {

	private final TeamRepository teamRepository;

	private final TeamMapper teamMapper;

	@Override
	protected void writeItem(TeamDto team) {
		teamRepository.save(teamMapper.dtoToEntity(team));
	}

	@Override
	protected String getItemTypeDescription() {
		return "team";
	}

}