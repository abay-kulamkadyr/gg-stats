package com.abe.gg_stats.dto.request.opendota.mapper;

import com.abe.gg_stats.dto.request.opendota.OpenDotaTeamDto;
import com.abe.gg_stats.entity.Team;
import java.util.List;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface OpenDotaTeamMapper {

	OpenDotaTeamDto entityToDTO(Team hero);

	List<OpenDotaTeamDto> entityToDTO(Iterable<Team> heroes);

	Team dtoToEntity(OpenDotaTeamDto hero);

	List<Team> dtoToEntity(Iterable<OpenDotaTeamDto> heroes);

}
