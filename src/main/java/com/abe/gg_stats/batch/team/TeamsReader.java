package com.abe.gg_stats.batch.team;

import com.abe.gg_stats.batch.BaseApiReader;
import com.abe.gg_stats.config.batch.BatchExpirationConfig;
import com.abe.gg_stats.repository.TeamRepository;
import com.abe.gg_stats.service.OpenDotaApiService;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class TeamsReader extends BaseApiReader {

	private final TeamRepository repository;

	private final OpenDotaApiService openDotaApiService;

	@Value("${app.batch.teams.pages:20}")
	private int pagesToFetch;

	private int currentPage = 0;

	@Autowired
	public TeamsReader(OpenDotaApiService openDotaApiService, BatchExpirationConfig expirationConfig,
			TeamRepository teamRepository) {
		super(expirationConfig);
		this.repository = teamRepository;
		this.openDotaApiService = openDotaApiService;
	}

	@Override
	protected Optional<Instant> findLatestUpdate() {
		return repository.findMaxUpdatedAt();
	}

	@Override
	protected String getExpirationConfigName() {
		return "teams";
	}

	@Override
	public JsonNode read() {
		if (!super.initialized) {
			Optional<Instant> latestUpdate = this.findLatestUpdate();
			boolean noRefreshNeeded = latestUpdate.isPresent() && super.noRefreshNeeded(latestUpdate.get());

			if (noRefreshNeeded) {
				return null;
			}
			super.initialized = true;
		}

		// If the current page is exhausted, or we haven't fetched any data yet, get the
		// next page
		if (super.dataIterator == null || !super.dataIterator.hasNext()) {
			if (currentPage >= pagesToFetch) {
				// We have fetched all the required pages, so we're done
				return null;
			}

			Optional<JsonNode> apiData = openDotaApiService.getTeamsPage(currentPage);

			if (apiData.isPresent() && apiData.get().isArray() && !apiData.get().isEmpty()) {
				super.dataIterator = apiData.get().elements();
				currentPage++;
			}
			else {
				return null;
			}
		}

		// Return the next item from the current page
		if (super.dataIterator.hasNext()) {
			return super.dataIterator.next();
		}

		// Fail-safe return
		return null;
	}

	@Override
	protected Optional<JsonNode> fetchApiData() {
		// This method is not used in the new pagination logic
		return Optional.empty();
	}

}