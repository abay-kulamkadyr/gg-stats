package com.abe.gg_stats.dto.mapper;

import com.abe.gg_stats.dto.TeamDto;
import com.abe.gg_stats.entity.Team;
import java.util.List;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TeamMapper {

	TeamDto entityToDTO(Team hero);

	List<TeamDto> entityToDTO(Iterable<Team> heroes);

	Team dtoToEntity(TeamDto hero);

	List<Team> dtoToEntity(Iterable<TeamDto> heroes);

}
