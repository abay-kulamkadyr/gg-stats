package com.abe.gg_stats.service;

import com.abe.gg_stats.repository.jdbc.AggregationDao;
import com.abe.gg_stats.util.LoggingConstants;
import com.abe.gg_stats.util.LoggingUtils;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AggregationService {

    private final OpenDotaApiService api;
    private final AggregationDao dao;

    public void refreshPatchesAndAggregations() {
        LoggingUtils.logOperationStart("aggregations", "type=weekly_patch");
        api.getPatches().ifPresent(json -> {
            dao.upsertPatches(json.toString());
        });
        dao.refreshTeamPicksView();
        dao.aggregateWeeklyHeroTrends();
        dao.aggregateWeeklyHeroPairs();
        LoggingUtils.logOperationSuccess("aggregations", "type=weekly_patch");
    }
}


