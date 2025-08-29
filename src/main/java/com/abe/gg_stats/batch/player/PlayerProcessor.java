package com.abe.gg_stats.batch.player;

import com.abe.gg_stats.batch.BaseProcessor;
import com.abe.gg_stats.entity.Player;
import com.abe.gg_stats.util.LoggingConstants;
import com.abe.gg_stats.util.LoggingUtils;
import com.abe.gg_stats.util.MDCLoggingContext;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import org.springframework.stereotype.Component;

@Component
public class PlayerProcessor extends BaseProcessor<JsonNode, Player> {

	@Override
	protected boolean isValidInput(JsonNode item) {
		// Set up validation context
		String correlationId = MDCLoggingContext.getOrCreateCorrelationId();
		MDCLoggingContext.updateContext("operationType", LoggingConstants.OPERATION_TYPE_BATCH);
		MDCLoggingContext.updateContext("batchType", "players");
		
		if (item == null || item.isNull()) {
			LoggingUtils.logWarning("Player JSON item is null", 
				"correlationId=" + correlationId);
			return false;
		}

		// Optional: check that at least one identifier exists
		boolean hasAccountId = item.has("account_id") && !item.get("account_id").isNull();
		boolean hasProfile = item.has("profile") && item.get("profile").isObject();

		if (!hasAccountId && !hasProfile) {
			LoggingUtils.logWarning("Player JSON item missing both account_id and profile", 
				"correlationId=" + correlationId,
				"item=" + item);
			return false;
		}

		if (hasProfile) {
			JsonNode profile = item.get("profile");

			// Validate required profile strings if present
			String steamId = getTextValue(profile, "steamid");
			String personName = getTextValue(profile, "personaname");

			if (steamId == null || personName == null) {
				LoggingUtils.logWarning("Invalid profile data, missing required fields", 
					"correlationId=" + correlationId,
					"profile=" + profile);
				return false;
			}
		}

		return true;
	}

	@Override
	protected Player processItem(JsonNode item) {
		Player player = new Player();
		processProfileData(player, item);
		processRootLevelData(player, item);
		return player;
	}

	@Override
	protected String getItemTypeDescription() {
		return "player account ID";
	}

	private void processProfileData(Player player, JsonNode data) {
		// Set up processing context
		String correlationId = MDCLoggingContext.getOrCreateCorrelationId();
		MDCLoggingContext.updateContext("operationType", LoggingConstants.OPERATION_TYPE_BATCH);
		MDCLoggingContext.updateContext("batchType", "players");
		
		JsonNode profileData = data.get("profile");
		if (profileData == null) {
			LoggingUtils.logWarning("No profile data found in API response", 
				"correlationId=" + correlationId);
			return;
		}

		// Basic profile fields
		player.setAccountId(getLongValue(profileData));
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

		LoggingUtils.logDebug("Processed profile data for player", 
			"correlationId=" + correlationId,
			"playerName=" + player.getPersonName());
	}

	private void processRootLevelData(Player player, JsonNode data) {
		// Set up processing context
		String correlationId = MDCLoggingContext.getOrCreateCorrelationId();
		MDCLoggingContext.updateContext("operationType", LoggingConstants.OPERATION_TYPE_BATCH);
		MDCLoggingContext.updateContext("batchType", "players");
		
		// Set the account ID from the root level
		if (data.has("account_id") && !data.get("account_id").isNull()) {
			player.setAccountId(data.get("account_id").asLong());
		}

		player.setRankTier(getIntValue(data, "rank_tier"));
		player.setLeaderboardRank(getIntValue(data, "leaderboard_rank"));
		LoggingUtils.logDebug("Processed root level data for player", 
			"correlationId=" + correlationId,
			"playerName=" + player.getPersonName());
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

	private Long getLongValue(JsonNode node) {
		return node.has("account_id") && !node.get("account_id").isNull() ? node.get("account_id").asLong() : null;
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
				String correlationId = MDCLoggingContext.getOrCreateCorrelationId();
				LoggingUtils.logWarning("Could not parse date time for field", 
					"correlationId=" + correlationId,
					"fieldName=" + fieldName, 
					"dateTimeStr=" + dateTimeStr);
				return null;
			}
		}
	}

}