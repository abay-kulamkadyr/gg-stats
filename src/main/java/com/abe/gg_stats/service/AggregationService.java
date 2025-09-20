package com.abe.gg_stats.service;

import com.abe.gg_stats.repository.jdbc.AggregationDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AggregationService {

	private final OpenDotaApiService api;

	private final AggregationDao dao;

	@Autowired
	public AggregationService(OpenDotaApiService api, AggregationDao dao) {
		this.api = api;
		this.dao = dao;
	}

	public void refreshPatchesAndAggregations() {
		api.getPatches().ifPresent(json -> {
			dao.upsertPatches(json.toString());
		});
		dao.refreshTeamPicksView();
		dao.aggregateWeeklyHeroTrends();
		dao.aggregateWeeklyHeroPairs();
		dao.refreshHeroItemPopularityView();
	}

}
