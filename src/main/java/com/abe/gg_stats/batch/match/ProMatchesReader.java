package com.abe.gg_stats.batch.match;

import com.abe.gg_stats.batch.BaseApiReader;
import com.abe.gg_stats.config.BatchExpirationConfig;
import com.abe.gg_stats.repository.jdbc.MatchIngestionDao;
import com.abe.gg_stats.service.OpenDotaApiService;
import com.abe.gg_stats.util.LoggingUtils;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import lombok.Setter;
import org.springframework.stereotype.Component;

@Setter
@Component
public class ProMatchesReader extends BaseApiReader {

	private static final int MAX_PAGES = 20; // safety cap per run

	private final MatchIngestionDao dao;

	public ProMatchesReader(OpenDotaApiService openDotaApiService, BatchExpirationConfig batchExpirationConfig,
			MatchIngestionDao dao) {
		super(openDotaApiService, batchExpirationConfig);
		this.dao = dao;
	}

	@Override
	protected void initialize() {
		List<JsonNode> all = new ArrayList<>();
		Long cursor = dao.getMinMatchId();
		int pages = 0;

		while (pages < MAX_PAGES) {
			Optional<JsonNode> page = openDotaApiService.getProMatchesPage(cursor);
			if (page.isEmpty() || !page.get().isArray() || page.get().isEmpty()) {
				break;
			}

			JsonNode arr = page.get();
			for (JsonNode node : arr) {
				all.add(node);
			}

			// prepare next page using min match_id
			long minId = Long.MAX_VALUE;
			for (JsonNode node : arr) {
				if (node.hasNonNull("match_id")) {
					long id = node.get("match_id").asLong();
					if (id < minId) {
						minId = id;
					}
				}
			}

			if (minId == Long.MAX_VALUE) {
				break;
			}
			cursor = minId;
			pages++;
		}

		LoggingUtils.logDebug("ProMatchesReader fetched pages", "pages=" + pages, "items=" + all.size());

		this.dataIterator = all.iterator();
	}

	@Override
	protected String getExpirationConfigName() {
		return "promatches";
	}

}
