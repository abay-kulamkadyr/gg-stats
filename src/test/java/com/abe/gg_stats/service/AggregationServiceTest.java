package com.abe.gg_stats.service;

import com.abe.gg_stats.repository.jdbc.AggregationDao;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.mockito.Mockito.*;

class AggregationServiceTest {

	private OpenDotaApiService api;

	private AggregationDao dao;

	private AggregationService service;

	@BeforeEach
	void setUp() {
		api = mock(OpenDotaApiService.class);
		dao = mock(AggregationDao.class);
		service = new AggregationService(api, dao);
	}

	@Test
	void refreshCallsUpsertWhenPatchesPresentAndAlwaysRefreshesAggregations() throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		JsonNode node = mapper.readTree("{\"v\":\"1\"}");
		when(api.getPatches()).thenReturn(Optional.of(node));

		service.refreshPatchesAndAggregations();

		verify(api).getPatches();
		verify(dao).upsertPatches(node.toString());
		verify(dao).refreshTeamPicksView();
		verify(dao).aggregateWeeklyHeroTrends();
		verify(dao).aggregateWeeklyHeroPairs();
		verify(dao).refreshHeroItemPopularityView();
		verifyNoMoreInteractions(api, dao);
	}

	@Test
	void refreshSkipsUpsertWhenNoPatchesButStillRefreshesAggregations() {
		when(api.getPatches()).thenReturn(Optional.empty());

		service.refreshPatchesAndAggregations();

		verify(api).getPatches();
		verify(dao, never()).upsertPatches(anyString());
		verify(dao).refreshTeamPicksView();
		verify(dao).aggregateWeeklyHeroTrends();
		verify(dao).aggregateWeeklyHeroPairs();
		verify(dao).refreshHeroItemPopularityView();
		verifyNoMoreInteractions(api, dao);
	}

}
