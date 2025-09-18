package com.abe.gg_stats.batch.player;

import com.abe.gg_stats.config.batch.BatchExpirationConfig;
import com.abe.gg_stats.dto.request.opendota.OpenDotaPlayerDto;
import com.abe.gg_stats.dto.request.opendota.mapper.OpenDotaPlayerResponseMapper;
import com.abe.gg_stats.dto.response.PlayerResponseDto;
import com.abe.gg_stats.entity.Player;
import com.abe.gg_stats.repository.PlayerRepository;
import com.abe.gg_stats.service.OpenDotaApiService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.Optional;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class PlayerProcessor implements ItemProcessor<Long, OpenDotaPlayerDto> {

	private final ObjectMapper objectMapper;

	private final OpenDotaPlayerResponseMapper openDotaPlayerResponseMapper;

	private final OpenDotaApiService openDotaApiService;

	private final PlayerRepository playerRepository;

	private final BatchExpirationConfig batchExpirationConfig;

	@Autowired
	public PlayerProcessor(OpenDotaApiService openDotaApiService, ObjectMapper objectMapper,
			OpenDotaPlayerResponseMapper openDotaPlayerResponseMapper, PlayerRepository playerRepository,
			BatchExpirationConfig batchExpirationConfig) {
		this.objectMapper = objectMapper;
		this.openDotaPlayerResponseMapper = openDotaPlayerResponseMapper;
		this.openDotaApiService = openDotaApiService;
		this.playerRepository = playerRepository;
		this.batchExpirationConfig = batchExpirationConfig;
	}

	@Override
	@Transactional(readOnly = true)
	public OpenDotaPlayerDto process(@NonNull Long accountId) throws Exception {

		Optional<Player> existingPlayer = playerRepository.findByAccountId(accountId);
		if (existingPlayer.isPresent() && noRefreshNeeded(existingPlayer.get().getUpdatedAt())) {
			return null;
		}

		Optional<JsonNode> apiResponse = openDotaApiService.getPlayer(accountId);
		if (apiResponse.isEmpty()) {
			return null;
		}

		JsonNode item = apiResponse.get();

		if (!isValidInput(item)) {
			return null;
		}

		return processItem(item);
	}

	private boolean noRefreshNeeded(Instant updatedAt) {
		if (updatedAt == null) {
			return false;
		}
		Instant expirationTime = updatedAt.plus(batchExpirationConfig.getDurationByConfigName("players"));
		return !Instant.now().isAfter(expirationTime);
	}

	protected OpenDotaPlayerDto processItem(JsonNode item) {
		try {
			PlayerResponseDto dto = objectMapper.treeToValue(item, PlayerResponseDto.class);
			return openDotaPlayerResponseMapper.toPlayerDto(dto);
		}
		catch (JsonProcessingException e) {
			return null;
		}
	}

	protected boolean isValidInput(JsonNode item) {
		if (item == null || item.isNull()) {
			return false;
		}

		// Optional: check that at least one identifier exists
		boolean hasAccountId = item.has("account_id") && !item.get("account_id").isNull();
		boolean hasProfile = item.has("profile") && item.get("profile").isObject();

		if (!hasAccountId && !hasProfile) {
			return false;
		}

		if (hasProfile) {
			JsonNode profile = item.get("profile");
			String steamId = getTextValue(profile, "steamid");
			String personName = getTextValue(profile, "personaname");

			return steamId != null && personName != null;
		}
		return true;
	}

	private String getTextValue(JsonNode node, String fieldName) {
		if (!node.has(fieldName) || node.get(fieldName).isNull()) {
			return null;
		}
		String value = node.get(fieldName).asText();
		return (value != null && !value.trim().isEmpty()) ? value : null;
	}

}