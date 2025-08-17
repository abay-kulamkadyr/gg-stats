package com.abe.gg_stats.batch.notablePlayer;

import com.abe.gg_stats.batch.BaseApiReader;
import com.abe.gg_stats.config.BatchExpirationConfig;
import com.abe.gg_stats.entity.NotablePlayer;
import com.abe.gg_stats.repository.NotablePlayerRepository;
import com.abe.gg_stats.service.OpenDotaApiService;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDateTime;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Iterator;
import java.util.Optional;

@Component
@Slf4j
public class NotablePlayersReader extends BaseApiReader<JsonNode> {

	private final NotablePlayerRepository notablePlayerRepository;

	@Autowired
	public NotablePlayersReader(OpenDotaApiService openDotaApiService, BatchExpirationConfig expirationConfig,
			NotablePlayerRepository notablePlayerRepository) {
		super(openDotaApiService, expirationConfig);
		this.notablePlayerRepository = notablePlayerRepository;
	}

	@Override
	protected void initialize() {
		Optional<LocalDateTime> latestUpdate = notablePlayerRepository.findMaxUpdatedAt();
		if (latestUpdate.isPresent() && super.noRefreshNeeded(latestUpdate.get())) {
			Duration expiration = super.getExpiration();
			log.info("Heroes data is up to date (last update: {}), expires in: {}", latestUpdate.get(),
					super.formatDuration(expiration));
			return;
		}

		Optional<JsonNode> proPlayersData = openDotaApiService.getProPlayers();
		if (proPlayersData.isPresent()) {
			this.dataIterator = proPlayersData.get().elements();
		}
		else {
			log.warn("Failed to initialize notable players reader - no data from API");
		}
	}

	@Override
	protected String getExpirationConfigName() {
		return "notablePlayers";
	}

}