package com.abe.gg_stats.batch;

import com.abe.gg_stats.repository.HeroRepository;
import com.abe.gg_stats.service.OpenDotaApiService;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemReader;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class HeroesReader implements ItemReader<JsonNode> {

	private final OpenDotaApiService openDotaApiService;

	private final HeroRepository heroRepository;

	private Iterator<JsonNode> heroIterator;

	private boolean initialized = false;

	@Override
	public JsonNode read() throws Exception {
		if (!initialized) {
			initialize();
		}

		if (heroIterator != null && heroIterator.hasNext()) {
			return heroIterator.next();
		}

		return null; // End of data or don't need to process
	}

	private void initialize() {
		List<LocalDateTime> allHeroIds = heroRepository.findAllUpdates();
		LocalDateTime latestUpdate = allHeroIds.stream().max(LocalDateTime::compareTo).orElse(null);
		LocalDateTime now = LocalDateTime.now();

		// do not fetch data unless 4 months has passed since the last update
		if (latestUpdate != null && ChronoUnit.MONTHS.between(latestUpdate, now) < 4) {
			log.info("All heroes data is up to date, nothing to update");
			return;
		}

		Optional<JsonNode> heroesData = openDotaApiService.getHeroes();
		if (heroesData.isPresent() && heroesData.get().isArray()) {
			heroIterator = heroesData.get().elements();
			log.info("Initialized heroes reader with {} heroes", heroesData.get().size());
		}
		else {
			log.warn("Failed to fetch heroes data from OpenDota API");
		}
		initialized = true;
	}

}
