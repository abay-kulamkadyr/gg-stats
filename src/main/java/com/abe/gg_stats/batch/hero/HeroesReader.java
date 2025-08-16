package com.abe.gg_stats.batch.hero;

import com.abe.gg_stats.batch.BaseApiReader;
import com.abe.gg_stats.config.BatchExpirationConfig;
import com.abe.gg_stats.repository.HeroRepository;
import com.abe.gg_stats.service.OpenDotaApiService;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

@Component
@Slf4j
public class HeroesReader extends BaseApiReader<JsonNode> {

	private final HeroRepository heroRepository;

	private final BatchExpirationConfig expirationConfig;

	public HeroesReader(OpenDotaApiService openDotaApiService, HeroRepository heroRepository,
			BatchExpirationConfig expirationConfig) {
		super(openDotaApiService);
		this.heroRepository = heroRepository;
		this.expirationConfig = expirationConfig;
	}

	@Override
	protected void initialize() {
		List<LocalDateTime> allHeroUpdates = heroRepository.findAllUpdates();
		LocalDateTime latestUpdate = allHeroUpdates.stream().max(LocalDateTime::compareTo).orElse(null);

		Duration expiration = getExpiration();
		if (!needsRefresh(latestUpdate, expiration)) {
			log.info("Heroes data is up to date (last update: {}), expires in: {}", latestUpdate,
					formatDuration(expiration));
			return;
		}

		// Fetch from API
		Optional<Iterator<JsonNode>> apiData = fetchFromApi(getApiEndpoint(), getDataTypeDescription());
		if (apiData.isPresent()) {
			this.dataIterator = apiData.get();
		}
		else {
			log.warn("Failed to initialize heroes reader - no data from API");
		}
	}

	@Override
	protected Iterator<JsonNode> convertApiResponseToIterator(JsonNode apiResponse) {
		return apiResponse.elements();
	}

	@Override
	protected Duration getExpiration() {
		return expirationConfig.getHeroesDuration();
	}

	@Override
	protected String getApiEndpoint() {
		return "/heroes";
	}

	@Override
	protected String getDataTypeDescription() {
		return "heroes";
	}

	/**
	 * Format duration for logging
	 */
	private String formatDuration(Duration duration) {
		long days = duration.toDays();
		long hours = duration.toHoursPart();
		long minutes = duration.toMinutesPart();

		if (days > 0) {
			return String.format("%d days, %d hours, %d minutes", days, hours, minutes);
		}
		else if (hours > 0) {
			return String.format("%d hours, %d minutes", hours, minutes);
		}
		else {
			return String.format("%d minutes", minutes);
		}
	}

}
