package com.abe.gg_stats.batch.hero;

import com.abe.gg_stats.batch.BaseApiReader;
import com.abe.gg_stats.config.BatchExpirationConfig;
import com.abe.gg_stats.repository.HeroRepository;
import com.abe.gg_stats.service.OpenDotaApiService;
import com.abe.gg_stats.util.LoggingUtils;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class HeroesReader extends BaseApiReader<JsonNode> {

	private final HeroRepository heroRepository;

	@Autowired
	public HeroesReader(OpenDotaApiService openDotaApiService, HeroRepository heroRepository,
			BatchExpirationConfig expirationConfig) {
		super(openDotaApiService, expirationConfig);
		this.heroRepository = heroRepository;
	}

	@Override
	protected void initialize() {
		Optional<LocalDateTime> latestUpdate = heroRepository.findMaxUpdatedAt();

		if (latestUpdate.isPresent() && noRefreshNeeded(latestUpdate.get())) {
			Duration expiration = super.getExpiration();
			LoggingUtils.logOperationSuccess("Heroes data in cache is valid", "lastUpdate=" + latestUpdate.get(),
					"expiresIn=" + super.formatDuration(expiration));
			return;
		}

		// Fetch from API
		Optional<JsonNode> apiData = openDotaApiService.getHeroes();

		if (apiData.isPresent()) {
			this.dataIterator = apiData.get().elements();
		}
		else {
			LoggingUtils.logWarning("Failed to initialize heroes reader - no data from API");
		}
	}

	@Override
	protected String getExpirationConfigName() {
		return "heroes";
	}

}
