package com.abe.gg_stats.dto.mapper;

import com.abe.gg_stats.dto.NotablePlayerDto;
import com.abe.gg_stats.entity.NotablePlayer;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface NotablePlayerMapper {

	NotablePlayerDto entityToDTO(NotablePlayer entity);

	List<NotablePlayerDto> entityToDTO(Iterable<NotablePlayer> entities);

	@Mapping(target = "team", ignore = true)
	NotablePlayer dtoToEntity(NotablePlayerDto dto);

	List<NotablePlayer> dtoToEntity(Iterable<NotablePlayerDto> dtos);

}
