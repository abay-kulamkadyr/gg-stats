package com.abe.gg_stats.dto.mapper;

import com.abe.gg_stats.dto.PlayerDto;
import com.abe.gg_stats.entity.Player;
import java.util.List;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PlayerMapper {

	PlayerDto entityToDTO(Player player);

	List<PlayerDto> entityToDTO(Iterable<Player> players);

	Player dtoToEntity(PlayerDto dto);

	List<Player> dtoToEntity(Iterable<PlayerDto> players);

}
