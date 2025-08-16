package com.abe.gg_stats.batch.team;

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
public class TeamsReader extends BaseApiReader<JsonNode> {

	private final BatchExpirationConfig expirationConfig;

	public TeamsReader(OpenDotaApiService openDotaApiService, BatchExpirationConfig expirationConfig) {
		super(openDotaApiService);
		this.expirationConfig = expirationConfig;
	}

	@Override
	protected void initialize() {
		// Teams data doesn't have expiration logic - always fetch from API
		Optional<Iterator<JsonNode>> teamsData = fetchFromApi(getApiEndpoint(), getDataTypeDescription());
		if (teamsData.isPresent()) {
			this.dataIterator = teamsData.get();
		}
		else {
			log.warn("Failed to initialize teams reader - no data from API");
		}
	}

	@Override
	protected Iterator<JsonNode> convertApiResponseToIterator(JsonNode apiResponse) {
		return apiResponse.elements();
	}

	@Override
	protected Duration getExpiration() {
		return expirationConfig.getTeamsDuration();
	}

	@Override
	protected String getApiEndpoint() {
		return "/teams";
	}

	@Override
	protected String getDataTypeDescription() {
		return "teams";
	}

}