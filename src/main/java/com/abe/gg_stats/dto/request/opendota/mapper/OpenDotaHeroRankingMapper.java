package com.abe.gg_stats.dto.request.opendota.mapper;

import com.abe.gg_stats.dto.request.opendota.OpenDotaHeroRankingDto;
import com.abe.gg_stats.entity.HeroRanking;
import java.util.List;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface OpenDotaHeroRankingMapper {

	OpenDotaHeroRankingDto entityToDTO(HeroRanking entity);

	List<OpenDotaHeroRankingDto> entityToDTO(Iterable<HeroRanking> entities);

	HeroRanking dtoToEntity(OpenDotaHeroRankingDto dto);

	List<HeroRanking> dtoToEntity(Iterable<OpenDotaHeroRankingDto> dtos);

}
