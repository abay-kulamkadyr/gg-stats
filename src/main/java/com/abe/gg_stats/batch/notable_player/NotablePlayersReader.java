package com.abe.gg_stats.batch.notable_player;

import com.abe.gg_stats.batch.BaseApiReader;
import com.abe.gg_stats.config.BatchExpirationConfig;
import com.abe.gg_stats.repository.NotablePlayerRepository;
import com.abe.gg_stats.service.OpenDotaApiService;
import com.abe.gg_stats.util.LoggingConstants;
import com.abe.gg_stats.util.LoggingUtils;
import com.abe.gg_stats.util.MDCLoggingContext;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDateTime;
import java.time.Duration;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class NotablePlayersReader extends BaseApiReader {

	private final NotablePlayerRepository notablePlayerRepository;

	public NotablePlayersReader(OpenDotaApiService openDotaApiService, BatchExpirationConfig expirationConfig,
			NotablePlayerRepository notablePlayerRepository) {
		super(openDotaApiService, expirationConfig);
		this.notablePlayerRepository = notablePlayerRepository;
	}

	@Override
	protected void initialize() {
		// Set up reader context
		String correlationId = MDCLoggingContext.getOrCreateCorrelationId();
		MDCLoggingContext.updateContext("operationType", LoggingConstants.OPERATION_TYPE_BATCH);
		MDCLoggingContext.updateContext("batchType", "notableplayers");

		LoggingUtils.logOperationStart("Initializing notable players reader", "correlationId=" + correlationId);

		Optional<LocalDateTime> latestUpdate = notablePlayerRepository.findMaxUpdatedAt();
		if (latestUpdate.isPresent() && super.noRefreshNeeded(latestUpdate.get())) {
			Duration expiration = super.getExpiration();
			LoggingUtils.logOperationSuccess("Notable players data in cache is valid", "correlationId=" + correlationId,
					"lastUpdate=" + latestUpdate.get(),
					"expiresIn=" + super.getFormattedDurationUntilExpiration(expiration));
			return;
		}

		// Fetch from API
		LoggingUtils.logOperationStart("Fetching notable players data from API", "correlationId=" + correlationId);

		Optional<JsonNode> proPlayersData = openDotaApiService.getProPlayers();
		if (proPlayersData.isPresent()) {
			this.dataIterator = proPlayersData.get().elements();
			LoggingUtils.logOperationSuccess("Successfully fetched notable players data from API",
					"correlationId=" + correlationId, "dataSize=" + proPlayersData.get().size());
		}
		else {
			LoggingUtils.logWarning("Failed to initialize notable players reader - no data from API",
					"correlationId=" + correlationId);
		}
	}

	@Override
	protected String getExpirationConfigName() {
		return "notableplayers";
	}

}