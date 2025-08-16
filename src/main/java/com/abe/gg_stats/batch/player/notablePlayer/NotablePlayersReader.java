package com.abe.gg_stats.batch.player.notablePlayer;

import com.abe.gg_stats.batch.BaseApiReader;
import com.abe.gg_stats.config.BatchExpirationConfig;
import com.abe.gg_stats.service.OpenDotaApiService;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Iterator;
import java.util.Optional;

@Component
@Slf4j
public class NotablePlayersReader extends BaseApiReader<JsonNode> {

	private final BatchExpirationConfig expirationConfig;

	public NotablePlayersReader(OpenDotaApiService openDotaApiService, BatchExpirationConfig expirationConfig) {
		super(openDotaApiService);
		this.expirationConfig = expirationConfig;
	}

	@Override
	protected void initialize() {
		// Notable players data doesn't have expiration logic - always fetch from API
		Optional<Iterator<JsonNode>> playersData = fetchFromApi(getApiEndpoint(), getDataTypeDescription());
		if (playersData.isPresent()) {
			this.dataIterator = playersData.get();
		}
		else {
			log.warn("Failed to initialize notable players reader - no data from API");
		}
	}

	@Override
	protected Iterator<JsonNode> convertApiResponseToIterator(JsonNode apiResponse) {
		return apiResponse.elements();
	}

	@Override
	protected Duration getExpiration() {
		return expirationConfig.getNotablePlayersDuration();
	}

	@Override
	protected String getApiEndpoint() {
		return "/proPlayers";
	}

	@Override
	protected String getDataTypeDescription() {
		return "notable players";
	}

}