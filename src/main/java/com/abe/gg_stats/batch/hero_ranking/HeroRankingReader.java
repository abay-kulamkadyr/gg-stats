package com.abe.gg_stats.batch.hero_ranking;

import com.abe.gg_stats.repository.HeroRepository;
import java.util.Iterator;
import java.util.List;
import org.springframework.batch.item.ItemReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class HeroRankingReader implements ItemReader<Integer> {

	private final HeroRepository heroRepository;

	private Iterator<Integer> heroIdIterator;

	@Autowired
	public HeroRankingReader(HeroRepository heroRepository) {
		this.heroRepository = heroRepository;
	}

	@Override
	public Integer read() throws Exception {
		if (heroIdIterator == null) {
			List<Integer> allHeroIds = heroRepository.findAllIds();
			this.heroIdIterator = allHeroIds.iterator();
		}

		if (heroIdIterator.hasNext()) {
			return heroIdIterator.next();
		}

		return null;
	}

}