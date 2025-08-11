package com.abe.gg_stats.batch;

import com.abe.gg_stats.service.OpenDotaApiService;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.Iterator;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemReader;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class TeamsReader implements ItemReader<JsonNode> {

	private final OpenDotaApiService openDotaApiService;

	private Iterator<JsonNode> teamIterator;

	private boolean initialized = false;

	@Override
	public JsonNode read() throws Exception {
		if (!initialized) {
			initialize();
		}

		if (teamIterator != null && teamIterator.hasNext()) {
			return teamIterator.next();
		}

		return null;
	}

	private void initialize() {
		Optional<JsonNode> teamsData = openDotaApiService.getTeams();
		if (teamsData.isPresent() && teamsData.get().isArray()) {
			teamIterator = teamsData.get().elements();
			log.info("Initialized teams reader with {} teams", teamsData.get().size());
		}
		else {
			log.warn("Failed to fetch teams data from OpenDota API");
		}
		initialized = true;
	}

}