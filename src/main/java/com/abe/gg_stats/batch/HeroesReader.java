package com.abe.gg_stats.batch;

import com.abe.gg_stats.service.OpenDotaApiService;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemReader;
import org.springframework.stereotype.Component;

import java.util.Iterator;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class HeroesReader implements ItemReader<JsonNode> {

	private final OpenDotaApiService openDotaApiService;

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

		return null; // End of data
	}

	private void initialize() {
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
