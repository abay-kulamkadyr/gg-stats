package com.abe.gg_stats.batch.team;

import com.abe.gg_stats.batch.BaseProcessor;
import com.abe.gg_stats.dto.request.opendota.OpenDotaTeamDto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class TeamProcessor extends BaseProcessor<OpenDotaTeamDto> {

	private final ObjectMapper objectMapper;

	@Autowired
	public TeamProcessor(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	@Override
	protected boolean isValidInput(JsonNode item) {

		if (item == null) {
			return false;
		}

		if (!item.has("team_id") || item.get("team_id").isNull()) {
			return false;
		}

		try {
			long teamId = item.get("team_id").asLong();
			if (teamId <= 0) {
				return false;
			}
		}
		catch (Exception e) {
			return false;
		}

		return true;
	}

	@Override
	protected OpenDotaTeamDto processItem(JsonNode item) {
		try {
			return objectMapper.treeToValue(item, OpenDotaTeamDto.class);
		}
		catch (Exception e) {
			return null;
		}
	}

}