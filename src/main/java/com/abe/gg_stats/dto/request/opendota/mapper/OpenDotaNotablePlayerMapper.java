package com.abe.gg_stats.dto.request.opendota.mapper;

import com.abe.gg_stats.dto.request.opendota.OpenDotaNotablePlayerDto;
import com.abe.gg_stats.entity.NotablePlayer;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface OpenDotaNotablePlayerMapper {

	OpenDotaNotablePlayerDto entityToDTO(NotablePlayer entity);

	List<OpenDotaNotablePlayerDto> entityToDTO(Iterable<NotablePlayer> entities);

	@Mapping(target = "team", ignore = true)
	NotablePlayer dtoToEntity(OpenDotaNotablePlayerDto dto);

	List<NotablePlayer> dtoToEntity(Iterable<OpenDotaNotablePlayerDto> dtos);

}
