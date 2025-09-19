package com.abe.gg_stats.batch.notable_player;

import com.abe.gg_stats.batch.BaseProcessor;
import com.abe.gg_stats.dto.request.opendota.OpenDotaNotablePlayerDto;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class NotablePlayerProcessor extends BaseProcessor<OpenDotaNotablePlayerDto> {

	private final ObjectMapper objectMapper;

	@Autowired
	public NotablePlayerProcessor(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	@Override
	public boolean isValidInput(JsonNode item) {
		if (item == null) {
			return false;
		}

		if (!item.has("account_id") || item.get("account_id").isNull()) {
			return false;
		}

		try {
			long accountId = item.get("account_id").asLong();
			if (accountId <= 0) {
				return false;
			}
		}
		catch (Exception e) {
			return false;
		}

		return true;
	}

	@Override
	protected OpenDotaNotablePlayerDto processItem(JsonNode item) {
		try {
			return objectMapper.treeToValue(item, OpenDotaNotablePlayerDto.class);
		}
		catch (JsonProcessingException e) {
			return null;
		}
	}

}