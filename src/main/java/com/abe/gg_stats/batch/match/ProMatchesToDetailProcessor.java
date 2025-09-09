package com.abe.gg_stats.batch.match;

import com.abe.gg_stats.service.OpenDotaApiService;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.Optional;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProMatchesToDetailProcessor implements ItemProcessor<JsonNode, JsonNode> {

	private final OpenDotaApiService api;

	@Override
	public JsonNode process(@NonNull JsonNode item) {
		if (!item.hasNonNull("match_id"))
			return null;
		long matchId = item.get("match_id").asLong();
		Optional<JsonNode> detail = api.getMatchDetail(matchId);
		return detail.orElse(null);
	}

}
