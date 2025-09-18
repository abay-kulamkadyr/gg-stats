package com.abe.gg_stats.batch.notable_player;

import com.abe.gg_stats.batch.BaseApiReader;
import com.abe.gg_stats.config.batch.BatchExpirationConfig;
import com.abe.gg_stats.repository.NotablePlayerRepository;
import com.abe.gg_stats.service.OpenDotaApiService;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class NotablePlayersReader extends BaseApiReader {

	private final NotablePlayerRepository notablePlayerRepository;

	private final OpenDotaApiService openDotaApiService;

	@Autowired
	public NotablePlayersReader(OpenDotaApiService openDotaApiService, BatchExpirationConfig expirationConfig,
			NotablePlayerRepository notablePlayerRepository) {
		super(expirationConfig);
		this.openDotaApiService = openDotaApiService;
		this.notablePlayerRepository = notablePlayerRepository;
	}

	@Override
	protected Optional<JsonNode> fetchApiData() {
		return openDotaApiService.getProPlayers();
	}

	@Override
	protected Optional<Instant> findLatestUpdate() {
		return notablePlayerRepository.findMaxUpdatedAt();
	}

	@Override
	protected String getExpirationConfigName() {
		return "notableplayers";
	}

}