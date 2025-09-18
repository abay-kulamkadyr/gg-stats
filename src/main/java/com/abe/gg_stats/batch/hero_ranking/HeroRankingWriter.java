package com.abe.gg_stats.batch.hero_ranking;

import com.abe.gg_stats.dto.request.opendota.OpenDotaHeroRankingDto;
import com.abe.gg_stats.dto.request.opendota.mapper.OpenDotaHeroRankingMapper;
import com.abe.gg_stats.entity.HeroRanking;
import com.abe.gg_stats.repository.HeroRankingRepository;
import java.util.List;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class HeroRankingWriter implements ItemWriter<List<OpenDotaHeroRankingDto>> {

	private final HeroRankingRepository heroRankingRepository;

	private final OpenDotaHeroRankingMapper heroRankingMapper;

	@Autowired
	public HeroRankingWriter(HeroRankingRepository heroRankingRepository, OpenDotaHeroRankingMapper heroRankingMapper) {
		this.heroRankingRepository = heroRankingRepository;
		this.heroRankingMapper = heroRankingMapper;
	}

	@Override
	public void write(Chunk<? extends List<OpenDotaHeroRankingDto>> chunk) throws Exception {
		if (chunk.isEmpty()) {
			return;
		}

		List<OpenDotaHeroRankingDto> allDtos = chunk.getItems().stream().flatMap(List::stream).toList();

		if (!allDtos.isEmpty()) {
			List<HeroRanking> entities = heroRankingMapper.dtoToEntity(allDtos);

			heroRankingRepository.saveAll(entities);
		}
	}

}