package com.abe.gg_stats.service;

import com.abe.gg_stats.entity.Player;
import com.abe.gg_stats.repository.PlayerRepository;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PlayerUpdateService {

	private final PlayerRepository playerRepository;

	private final OpenDotaApiService openDotaApiService;

	@Transactional
	public Player updatePlayerInfo(Long accountId) {
		log.info("Updating player info for account_id: {}", accountId);

		try {
			Optional<Player> existingPlayerOpt = playerRepository.findById(accountId);
			LocalDateTime now = LocalDateTime.now();

			if (existingPlayerOpt.isPresent() && existingPlayerOpt.get().getUpdatedAt() != null) {
				Player player = existingPlayerOpt.get();
				long daysSinceUpdate = ChronoUnit.DAYS.between(player.getUpdatedAt(), now);

				if (daysSinceUpdate < 7) {
					log.info("Player {} data is accurate, skipping ", accountId);
					return null;
				}
			}

			// Fetch player info from OpenDota API
			Optional<JsonNode> playerData = openDotaApiService.getPlayer(accountId);
			if (playerData.isEmpty()) {
				log.warn("No data received from OpenDota API for account_id: {}", accountId);
				// throw new PlayerProcessingException("No API data received for account:
				// " + accountId);
				return null;
			}

			JsonNode data = playerData.get();
			log.debug("Received API data for account_id: {}, data: {}", accountId, data.toString());

			Player player = existingPlayerOpt.orElse(new Player());
			player.setAccountId(accountId);

			// Process profile data
			processProfileData(player, data);

			// Process root level data
			processRootLevelData(player, data);

			// Save player
			Player savedPlayer = playerRepository.save(player);
			log.info("Successfully updated player info for account_id: {} with personaname: {}", accountId,
					savedPlayer.getPersonName());

			return savedPlayer;

		}
		catch (PlayerProcessingException e) {
			// Re-throw as-is
			throw e;
		}
		catch (Exception e) {
			log.error("Failed to update player info for account_id: {}", accountId, e);
			throw new PlayerProcessingException("Failed to update player: " + accountId, e);
		}
	}

	private void processProfileData(Player player, JsonNode data) {
		JsonNode profileData = data.get("profile");
		if (profileData == null) {
			log.warn("No profile data found in API response");
			return;
		}

		// Basic profile fields
		player.setSteamId(getTextValue(profileData, "steamid"));
		player.setAvatar(getTextValue(profileData, "avatar"));
		player.setAvatarMedium(getTextValue(profileData, "avatarmedium"));
		player.setAvatarFull(getTextValue(profileData, "avatarfull"));
		player.setProfileUrl(getTextValue(profileData, "profileurl"));
		player.setPersonName(getTextValue(profileData, "personaname"));

		// Date fields
		player.setLastLogin(parseDateTime(profileData, "last_login"));
		player.setFullHistoryTime(parseDateTime(profileData, "full_history_time"));
		player.setLastMatchTime(parseDateTime(profileData, "last_match_time"));

		// Numeric and boolean fields
		player.setCheese(getIntValue(profileData, "cheese"));
		player.setFhUnavailable(getBooleanValue(profileData, "fh_unavailable"));
		player.setLocCountryCode(getTextValue(profileData, "loccountrycode"));
		player.setPlus(getBooleanValue(profileData, "plus"));

		log.debug("Processed profile data for player: {}", player.getPersonName());
	}

	private void processRootLevelData(Player player, JsonNode data) {
		player.setRankTier(getIntValue(data, "rank_tier"));
		player.setLeaderboardRank(getIntValue(data, "leaderboard_rank"));
		log.debug("Processed root level data for player: {}", player.getPersonName());
	}

	private String getTextValue(JsonNode node, String fieldName) {
		if (!node.has(fieldName) || node.get(fieldName).isNull()) {
			return null;
		}
		String value = node.get(fieldName).asText();
		return (value != null && !value.trim().isEmpty()) ? value : null;
	}

	private Integer getIntValue(JsonNode node, String fieldName) {
		return node.has(fieldName) && !node.get(fieldName).isNull() ? node.get(fieldName).asInt() : null;
	}

	private Boolean getBooleanValue(JsonNode node, String fieldName) {
		return node.has(fieldName) && !node.get(fieldName).isNull() ? node.get(fieldName).asBoolean() : null;
	}

	private LocalDateTime parseDateTime(JsonNode data, String fieldName) {
		if (!data.has(fieldName) || data.get(fieldName).isNull()) {
			return null;
		}

		String dateTimeStr = data.get(fieldName).asText();
		if (dateTimeStr == null || dateTimeStr.trim().isEmpty()) {
			return null;
		}

		try {
			// Try parsing as Unix timestamp first
			long timestamp = Long.parseLong(dateTimeStr);
			return LocalDateTime.ofEpochSecond(timestamp, 0, java.time.ZoneOffset.UTC);
		}
		catch (NumberFormatException e) {
			// Try parsing as ISO date time string
			try {
				return LocalDateTime.parse(dateTimeStr, DateTimeFormatter.ISO_DATE_TIME);
			}
			catch (DateTimeParseException e2) {
				log.warn("Could not parse date time for field {}: {}", fieldName, dateTimeStr);
				return null;
			}
		}
	}

	public static class PlayerProcessingException extends RuntimeException {

		public PlayerProcessingException(String message) {
			super(message);
		}

		public PlayerProcessingException(String message, Throwable cause) {
			super(message, cause);
		}

	}

}
