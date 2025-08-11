package com.abe.gg_stats.batch;

import com.abe.gg_stats.entity.Hero;
import com.abe.gg_stats.repository.HeroRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class HeroWriter implements ItemWriter<Hero> {

	private final HeroRepository heroRepository;

	@Override
	public void write(Chunk<? extends Hero> chunk) throws Exception {
		for (Hero hero : chunk) {
			try {
				heroRepository.save(hero);
				log.debug("Saved hero: {} ({})", hero.getLocalizedName(), hero.getId());
			}
			catch (Exception e) {
				log.error("Error saving hero: {}", hero.getName(), e);
			}
		}
		log.info("Processed {} heroes", chunk.size());
	}

}
