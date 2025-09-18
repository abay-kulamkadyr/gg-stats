package com.abe.gg_stats.batch.team;

import com.abe.gg_stats.batch.BaseWriter;
import com.abe.gg_stats.dto.request.opendota.OpenDotaTeamDto;
import com.abe.gg_stats.dto.request.opendota.mapper.OpenDotaTeamMapper;
import com.abe.gg_stats.entity.Team;
import com.abe.gg_stats.repository.TeamRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class TeamWriter extends BaseWriter<OpenDotaTeamDto, Team> {

	private final OpenDotaTeamMapper teamMapper;

	@Autowired
	public TeamWriter(TeamRepository teamRepository, OpenDotaTeamMapper teamMapper) {
		super(teamRepository);
		this.teamMapper = teamMapper;
	}

	@Override
	protected Team dtoToEntity(OpenDotaTeamDto dto) {
		return teamMapper.dtoToEntity(dto);
	}

}