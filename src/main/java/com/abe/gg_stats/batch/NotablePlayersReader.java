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
public class NotablePlayersReader implements ItemReader<JsonNode> {

	private final OpenDotaApiService openDotaApiService;

	private Iterator<JsonNode> proPlayerIterator;

	private boolean initialized = false;

	@Override
	public JsonNode read() throws Exception {
		if (!initialized) {
			initialize();
		}

		if (proPlayerIterator != null && proPlayerIterator.hasNext()) {
			return proPlayerIterator.next();
		}

		return null;
	}

	private void initialize() {
		Optional<JsonNode> proPlayersData = openDotaApiService.getProPlayers();
		if (proPlayersData.isPresent() && proPlayersData.get().isArray()) {
			proPlayerIterator = proPlayersData.get().elements();
			log.info("Initialized pro players reader with {} players", proPlayersData.get().size());
		}
		else {
			log.warn("Failed to fetch pro players data from OpenDota API");
		}
		initialized = true;
	}

}