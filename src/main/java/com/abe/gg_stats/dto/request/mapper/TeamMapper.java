package com.abe.gg_stats.dto.request.mapper;

import com.abe.gg_stats.dto.response.TeamDto;
import com.abe.gg_stats.entity.Team;
import org.springframework.stereotype.Component;

@Component
public class TeamMapper {

	public TeamDto toTeamDto(Team t) {
		return new TeamDto(t.getTeamId(), t.getName(), t.getTag(), t.getLogoUrl(), t.getRating());
	}

}
