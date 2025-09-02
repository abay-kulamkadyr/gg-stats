package com.abe.gg_stats.dto.mapper;

import com.abe.gg_stats.dto.HeroRankingDto;
import com.abe.gg_stats.entity.HeroRanking;
import java.util.List;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface HeroRankingMapper {

	HeroRankingDto entityToDTO(HeroRanking entity);

	List<HeroRankingDto> entityToDTO(Iterable<HeroRanking> entities);

	HeroRanking dtoToEntity(HeroRankingDto dto);

	List<HeroRanking> dtoToEntity(Iterable<HeroRankingDto> dtos);

}
