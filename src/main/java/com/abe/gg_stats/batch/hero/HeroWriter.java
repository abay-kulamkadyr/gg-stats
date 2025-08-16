package com.abe.gg_stats.batch.hero;

import com.abe.gg_stats.batch.BaseWriter;
import com.abe.gg_stats.entity.Hero;
import com.abe.gg_stats.repository.HeroRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class HeroWriter extends BaseWriter<Hero> {

	private final HeroRepository heroRepository;

	public HeroWriter(HeroRepository heroRepository) {
		this.heroRepository = heroRepository;
	}

	@Override
	protected void writeItem(Hero hero) throws Exception {
		heroRepository.save(hero);
	}

	@Override
	protected String getItemTypeDescription() {
		return "hero";
	}

}
