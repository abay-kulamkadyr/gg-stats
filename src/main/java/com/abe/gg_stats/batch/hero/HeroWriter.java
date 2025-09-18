package com.abe.gg_stats.batch.hero;

import com.abe.gg_stats.batch.BaseWriter;
import com.abe.gg_stats.dto.request.opendota.OpenDotaHeroDto;
import com.abe.gg_stats.dto.request.opendota.mapper.OpenDotaHeroMapper;
import com.abe.gg_stats.entity.Hero;
import com.abe.gg_stats.repository.HeroRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class HeroWriter extends BaseWriter<OpenDotaHeroDto, Hero> {

	private final OpenDotaHeroMapper heroMapper;

	@Autowired
	public HeroWriter(HeroRepository heroRepository, OpenDotaHeroMapper heroMapper) {
		super(heroRepository);
		this.heroMapper = heroMapper;
	}

	@Override
	protected Hero dtoToEntity(OpenDotaHeroDto openDotaHeroDto) {
		return heroMapper.dtoToEntity(openDotaHeroDto);
	}

}
