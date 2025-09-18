package com.abe.gg_stats.dto.request.opendota.mapper;

import com.abe.gg_stats.dto.request.opendota.OpenDotaHeroDto;
import com.abe.gg_stats.entity.Hero;
import java.util.List;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface OpenDotaHeroMapper {

	OpenDotaHeroDto entityToDTO(Hero hero);

	List<OpenDotaHeroDto> entityToDTO(Iterable<Hero> heroes);

	Hero dtoToEntity(OpenDotaHeroDto hero);

	List<Hero> dtoToEntity(Iterable<OpenDotaHeroDto> heroes);

}
