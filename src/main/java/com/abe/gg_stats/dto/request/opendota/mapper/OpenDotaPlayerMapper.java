package com.abe.gg_stats.dto.request.opendota.mapper;

import com.abe.gg_stats.dto.request.opendota.OpenDotaPlayerDto;
import com.abe.gg_stats.entity.Player;
import java.util.List;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface OpenDotaPlayerMapper {

	OpenDotaPlayerDto entityToDTO(Player player);

	List<OpenDotaPlayerDto> entityToDTO(Iterable<Player> players);

	Player dtoToEntity(OpenDotaPlayerDto dto);

	List<Player> dtoToEntity(Iterable<OpenDotaPlayerDto> players);

}
