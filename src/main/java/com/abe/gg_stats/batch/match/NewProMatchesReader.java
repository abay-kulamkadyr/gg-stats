package com.abe.gg_stats.batch.match;

import com.abe.gg_stats.repository.jdbc.MatchIngestionDao;
import com.abe.gg_stats.service.OpenDotaApiService;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.StreamSupport;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.ItemStreamReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class NewProMatchesReader implements ItemStreamReader<JsonNode> {

	private static final String CURSOR_KEY = "new-matches.cursor";

	private final OpenDotaApiService openDotaApiService;

	private final MatchIngestionDao dao;

	private Long maxMatchIdInDb;

	private Long cursor;

	private Iterator<JsonNode> it;

	private boolean exhausted;

	@Autowired
	public NewProMatchesReader(OpenDotaApiService openDotaApiService, MatchIngestionDao dao) {
		this.openDotaApiService = openDotaApiService;
		this.dao = dao;
	}

	@Override
	public void open(ExecutionContext executionContext) throws ItemStreamException {
		this.maxMatchIdInDb = dao.getMaxMatchId();
		this.cursor = executionContext.containsKey(CURSOR_KEY) ? executionContext.getLong(CURSOR_KEY) : null;
		this.exhausted = false;
	}

	@Override
	public void update(ExecutionContext executionContext) throws ItemStreamException {
		if (cursor != null) {
			executionContext.putLong(CURSOR_KEY, cursor);
		}
	}

	@Override
	public JsonNode read() {
		if (exhausted) {
			return null;
		}
		if (it == null || !it.hasNext()) {
			if (!fetchNextPage()) {
				exhausted = true;
				return null;
			}
		}
		return it.hasNext() ? it.next() : null;
	}

	private boolean fetchNextPage() {
		Optional<JsonNode> pageOpt = openDotaApiService.getProMatchesPage(cursor);
		if (pageOpt.isEmpty() || !pageOpt.get().isArray() || pageOpt.get().isEmpty()) {
			return false;
		}

		List<JsonNode> page = StreamSupport.stream(pageOpt.get().spliterator(), false).toList();

		// advance cursor to the smallest id on the page (last element) to walk backwards
		// next call
		JsonNode last = page.getLast();
		if (last != null && last.hasNonNull("match_id")) {
			this.cursor = last.get("match_id").asLong();
		}

		// filter strictly newer than DB max
		List<JsonNode> newer = page.stream()
			.filter(n -> n.hasNonNull("match_id")
					&& (maxMatchIdInDb == null || n.get("match_id").asLong() > maxMatchIdInDb))
			.toList();

		if (newer.isEmpty()) {
			return false;
		}

		this.it = newer.iterator();
		return true;
	}

}