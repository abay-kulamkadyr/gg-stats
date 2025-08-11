package com.abe.gg_stats.batch;

import com.abe.ggstats.service.OpenDotaApiService;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemReader;
import org.springframework.stereotype.Component;

import java.util.Iterator;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class LeaderboardReader implements ItemReader<JsonNode> {

  private final OpenDotaApiService openDotaApiService;
  private Iterator<JsonNode> leaderboardIterator;
  private boolean initialized = false;

  @Override
  public JsonNode read() throws Exception {
    if (!initialized) {
      initialize();
    }

    if (leaderboardIterator != null && leaderboardIterator.hasNext()) {
      return leaderboardIterator.next();
    }

    return null;
  }

  private void initialize() {
    // Fetch leaderboard for different regions
    String[] regions = {"americas", "europe", "se_asia", "china"};

    for (String region : regions) {
      Optional<JsonNode> leaderboardData = openDotaApiService.getLeaderboard(region);
      if (leaderboardData.isPresent() && leaderboardData.get().isArray()) {
        leaderboardIterator = leaderboardData.get().elements();
        log.info("Initialized leaderboard reader with {} entries from {}",
            leaderboardData.get().size(), region);
        break; // Use first successful region for now
      }
    }

    if (leaderboardIterator == null) {
      log.warn("Failed to fetch leaderboard data from OpenDota API");
    }
    initialized = true;
  }
}
