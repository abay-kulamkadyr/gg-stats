package com.abe.gg_stats.dto;

import com.abe.gg_stats.entity.Hero;
import java.util.List;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface HeroMapper {

	HeroDto entityToDTO(Hero hero);

	List<HeroDto> entityToDTO(Iterable<Hero> heroes);

	Hero dtoToEntity(HeroDto hero);

	List<Hero> dtoToEntity(Iterable<HeroDto> heroes);

}
