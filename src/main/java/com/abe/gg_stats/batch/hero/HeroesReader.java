package com.abe.gg_stats.batch.hero;

import com.abe.gg_stats.batch.BaseApiReader;
import com.abe.gg_stats.config.BatchExpirationConfig;
import com.abe.gg_stats.repository.HeroRepository;
import com.abe.gg_stats.service.OpenDotaApiService;
import com.abe.gg_stats.util.LoggingConstants;
import com.abe.gg_stats.util.LoggingUtils;
import com.abe.gg_stats.util.MDCLoggingContext;
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
		// Set up reader context
		String correlationId = MDCLoggingContext.getOrCreateCorrelationId();
		MDCLoggingContext.updateContext("operationType", LoggingConstants.OPERATION_TYPE_BATCH);
		MDCLoggingContext.updateContext("batchType", "heroes");
		
		LoggingUtils.logOperationStart("Initializing heroes reader", 
			"correlationId=" + correlationId);
		
		Optional<LocalDateTime> latestUpdate = heroRepository.findMaxUpdatedAt();

		if (latestUpdate.isPresent() && noRefreshNeeded(latestUpdate.get())) {
			Duration expiration = super.getExpiration();
			LoggingUtils.logOperationSuccess("Heroes data in cache is valid", 
				"correlationId=" + correlationId,
				"lastUpdate=" + latestUpdate.get(),
				"expiresIn=" + super.formatDuration(expiration));
			return;
		}

		// Fetch from API
		LoggingUtils.logOperationStart("Fetching heroes data from API", 
			"correlationId=" + correlationId);
		
		Optional<JsonNode> apiData = openDotaApiService.getHeroes();

		if (apiData.isPresent()) {
			this.dataIterator = apiData.get().elements();
			LoggingUtils.logOperationSuccess("Successfully fetched heroes data from API", 
				"correlationId=" + correlationId,
				"dataSize=" + apiData.get().size());
		}
		else {
			LoggingUtils.logWarning("Failed to initialize heroes reader - no data from API", 
				"correlationId=" + correlationId);
		}
	}

	@Override
	protected String getExpirationConfigName() {
		return "heroes";
	}

}
