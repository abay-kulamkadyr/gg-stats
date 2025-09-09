package com.abe.gg_stats.batch.team;

import com.abe.gg_stats.batch.BaseApiReader;
import com.abe.gg_stats.config.BatchExpirationConfig;
import com.abe.gg_stats.repository.TeamRepository;
import com.abe.gg_stats.service.OpenDotaApiService;
import com.abe.gg_stats.util.LoggingConstants;
import com.abe.gg_stats.util.LoggingUtils;
import com.abe.gg_stats.util.MDCLoggingContext;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class TeamsReader extends BaseApiReader {

	private final TeamRepository teamRepository;

	public TeamsReader(OpenDotaApiService openDotaApiService, BatchExpirationConfig expirationConfig,
			TeamRepository teamRepository) {
		super(openDotaApiService, expirationConfig);
		this.teamRepository = teamRepository;
	}

	@Override
	protected void initialize() {
		// Set up reader context
		String correlationId = MDCLoggingContext.getOrCreateCorrelationId();
		MDCLoggingContext.updateContext("operationType", LoggingConstants.OPERATION_TYPE_BATCH);
		MDCLoggingContext.updateContext("batchType", "teams");

		LoggingUtils.logOperationStart("Initializing teams reader", "correlationId=" + correlationId);

		// Teams data doesn't have expiration logic - always fetch from API
		Optional<Instant> latestUpdate = teamRepository.findMaxUpdatedAt();

		if (latestUpdate.isPresent() && super.noRefreshNeeded(latestUpdate.get())) {
			Duration expiration = super.getExpiration();
			LoggingUtils.logOperationSuccess("Teams data in cache is valid", "correlationId=" + correlationId,
					"lastUpdate=" + latestUpdate.get(),
					"expiresIn=" + super.getFormattedDurationUntilExpiration(expiration));
			return;
		}

		// Fetch multi-page from API (default 20 pages)
		LoggingUtils.logOperationStart("Fetching teams data from API (multi-page)", "correlationId=" + correlationId);

		Optional<JsonNode> teamsData = openDotaApiService.getTeamsAllPages(20);
		if (teamsData.isPresent()) {
			this.dataIterator = teamsData.get().elements();
			LoggingUtils.logOperationSuccess("Successfully fetched teams data from API (multi-page)",
					"correlationId=" + correlationId, "dataSize=" + teamsData.get().size());
		}
		else {
			LoggingUtils.logWarning("Failed to initialize teams reader - no data from API",
					"correlationId=" + correlationId);
		}
	}

	@Override
	protected String getExpirationConfigName() {
		return "teams";
	}

}