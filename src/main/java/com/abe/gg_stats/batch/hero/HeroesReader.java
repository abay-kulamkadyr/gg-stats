package com.abe.gg_stats.batch.hero;

import com.abe.gg_stats.batch.BaseApiReader;
import com.abe.gg_stats.config.batch.BatchExpirationConfig;
import com.abe.gg_stats.repository.HeroRepository;
import com.abe.gg_stats.service.OpenDotaApiService;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class HeroesReader extends BaseApiReader {

	private final OpenDotaApiService openDotaApiService;

	private final HeroRepository repository;

	@Autowired
	public HeroesReader(OpenDotaApiService openDotaApiService, HeroRepository heroRepository,
			BatchExpirationConfig expirationConfig) {
		super(expirationConfig);
		this.openDotaApiService = openDotaApiService;
		this.repository = heroRepository;
	}

	@Override
	protected Optional<JsonNode> fetchApiData() {
		return openDotaApiService.getHeroes();
	}

	@Override
	protected Optional<Instant> findLatestUpdate() {
		return repository.findMaxUpdatedAt();
	}

	@Override
	protected String getExpirationConfigName() {
		return "heroes";
	}

}
