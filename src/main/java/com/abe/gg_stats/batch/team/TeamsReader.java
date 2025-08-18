package com.abe.gg_stats.batch.team;

import com.abe.gg_stats.batch.BaseApiReader;
import com.abe.gg_stats.config.BatchExpirationConfig;
import com.abe.gg_stats.repository.TeamRepository;
import com.abe.gg_stats.service.OpenDotaApiService;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDateTime;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

@Component
@Slf4j
public class TeamsReader extends BaseApiReader<JsonNode> {

	private final BatchExpirationConfig expirationConfig;

	private final TeamRepository teamRepository;

	@Autowired
	public TeamsReader(OpenDotaApiService openDotaApiService, BatchExpirationConfig expirationConfig,
			TeamRepository teamRepository) {
		super(openDotaApiService, expirationConfig);
		this.expirationConfig = expirationConfig;
		this.teamRepository = teamRepository;
	}

	@Override
	protected void initialize() {
		// Teams data doesn't have expiration logic - always fetch from API
		Optional<LocalDateTime> latestUpdate = teamRepository.findMaxUpdatedAt();

		if (latestUpdate.isPresent() && super.noRefreshNeeded(latestUpdate.get())) {
			Duration expiration = super.getExpiration();
			log.info("Teams data is up to date (last update: {}), expires in: {}", latestUpdate.get(),
					super.formatDuration(expiration));
			return;
		}

		Optional<JsonNode> teamsData = openDotaApiService.getTeams();
		if (teamsData.isPresent()) {
			this.dataIterator = teamsData.get().elements();
		}
		else {
			log.warn("Failed to initialize teams reader - no data from API");
		}
	}

	@Override
	protected Duration getExpiration() {
		return expirationConfig.getTeamsDuration();
	}

	@Override
	protected String getExpirationConfigName() {
		return "teams";
	}

}