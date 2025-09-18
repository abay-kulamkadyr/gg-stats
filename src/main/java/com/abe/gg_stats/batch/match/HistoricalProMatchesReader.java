package com.abe.gg_stats.batch.match;

import com.abe.gg_stats.repository.jdbc.MatchIngestionDao;
import com.abe.gg_stats.service.OpenDotaApiService;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.Collections;
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
public class HistoricalProMatchesReader implements ItemStreamReader<JsonNode> {

	private static final String CURSOR_KEY = "historical-matches.cursor";

	private final OpenDotaApiService openDotaApiService;

	private final MatchIngestionDao dao;

	private Iterator<JsonNode> matchIterator;

	private Long currentCursor;

	@Autowired
	public HistoricalProMatchesReader(OpenDotaApiService openDotaApiService, MatchIngestionDao dao) {
		this.openDotaApiService = openDotaApiService;
		this.dao = dao;
	}

	@Override
	public void open(ExecutionContext executionContext) throws ItemStreamException {
		if (executionContext.containsKey(CURSOR_KEY)) {
			this.currentCursor = executionContext.getLong(CURSOR_KEY);
		}
	}

	@Override
	public void update(ExecutionContext executionContext) throws ItemStreamException {
		if (this.currentCursor != null) {
			executionContext.putLong(CURSOR_KEY, this.currentCursor);
		}
	}

	@Override
	public JsonNode read() {
		if (matchIterator == null || !matchIterator.hasNext()) {
			fetchNextPage();
		}
		return matchIterator != null && matchIterator.hasNext() ? matchIterator.next() : null;
	}

	private void fetchNextPage() {
		// Start from the lowest ID we have, then page backwards in history using
		// less_than_match_id
		if (currentCursor == null) {
			currentCursor = dao.getMinMatchId();
		}
		if (currentCursor == null) {
			matchIterator = Collections.emptyIterator();
			return;
		}

		Optional<JsonNode> page = openDotaApiService.getProMatchesPage(currentCursor);
		if (page.isEmpty() || !page.get().isArray() || page.get().isEmpty()) {
			matchIterator = Collections.emptyIterator();
			return;
		}

		List<JsonNode> pageList = StreamSupport.stream(page.get().spliterator(), false).toList();

		// Update cursor to the smallest match_id on this page (last element) to continue
		// paging
		JsonNode last = pageList.getLast();
		if (last != null && last.hasNonNull("match_id")) {
			this.currentCursor = last.get("match_id").asLong();
		}

		matchIterator = pageList.iterator();
	}

}