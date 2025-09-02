package com.abe.gg_stats.batch.player;

import com.abe.gg_stats.batch.BaseProcessor;
import com.abe.gg_stats.dto.PlayerDto;
import com.abe.gg_stats.util.LoggingConstants;
import com.abe.gg_stats.util.LoggingUtils;
import com.abe.gg_stats.util.MDCLoggingContext;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import org.springframework.stereotype.Component;

@Component
public class PlayerProcessor extends BaseProcessor<PlayerDto> {

	@Override
	protected boolean isValidInput(JsonNode item) {
		// Set up validation context
		String correlationId = MDCLoggingContext.getOrCreateCorrelationId();
		MDCLoggingContext.updateContext("operationType", LoggingConstants.OPERATION_TYPE_BATCH);
		MDCLoggingContext.updateContext("batchType", "players");

		if (item == null || item.isNull()) {
			LoggingUtils.logWarning("Player JSON item is null", "correlationId=" + correlationId);
			return false;
		}

		// Optional: check that at least one identifier exists
		boolean hasAccountId = item.has("account_id") && !item.get("account_id").isNull();
		boolean hasProfile = item.has("profile") && item.get("profile").isObject();

		if (!hasAccountId && !hasProfile) {
			LoggingUtils.logWarning("Player JSON item missing both account_id and profile",
					"correlationId=" + correlationId, "item=" + item);
			return false;
		}

		if (hasProfile) {
			JsonNode profile = item.get("profile");

			// Validate required profile strings if present
			String steamId = getTextValue(profile, "steamid");
			String personName = getTextValue(profile, "personaname");

			if (steamId == null || personName == null) {
				LoggingUtils.logWarning("Invalid profile data, missing required fields",
						"correlationId=" + correlationId, "profile=" + profile);
				return false;
			}
		}

		return true;
	}

	@Override
	protected PlayerDto processItem(JsonNode item) {
		Long accountId = null;
		if (item.has("account_id") && !item.get("account_id").isNull()) {
			accountId = item.get("account_id").asLong();
		}

		JsonNode profileData = item.get("profile");
		// Fallback: some payloads include account_id under profile
		if (accountId == null && profileData != null && profileData.has("account_id")
				&& !profileData.get("account_id").isNull()) {
			accountId = profileData.get("account_id").asLong();
		}

		String steamId = profileData != null ? getTextValue(profileData, "steamid") : null;
		String avatar = profileData != null ? getTextValue(profileData, "avatar") : null;
		String avatarMedium = profileData != null ? getTextValue(profileData, "avatarmedium") : null;
		String avatarFull = profileData != null ? getTextValue(profileData, "avatarfull") : null;
		String profileUrl = profileData != null ? getTextValue(profileData, "profileurl") : null;
		String personName = profileData != null ? getTextValue(profileData, "personaname") : null;
		Instant lastLogin = profileData != null ? parseDateTime(profileData, "last_login") : null;
		Instant fullHistoryTime = profileData != null ? parseDateTime(profileData, "full_history_time") : null;
		Instant lastMatchTime = profileData != null ? parseDateTime(profileData, "last_match_time") : null;
		Integer cheese = profileData != null ? getIntValue(profileData, "cheese") : null;
		Boolean fhUnavailable = profileData != null ? getBooleanValue(profileData, "fh_unavailable") : null;
		String locCountryCode = profileData != null ? getTextValue(profileData, "loccountrycode") : null;
		Boolean plus = profileData != null ? getBooleanValue(profileData, "plus") : null;

		Integer rankTier = getIntValue(item, "rank_tier");
		Integer leaderboardRank = getIntValue(item, "leaderboard_rank");

		return new PlayerDto(accountId, steamId, avatar, avatarMedium, avatarFull, profileUrl, personName, lastLogin,
				fullHistoryTime, cheese, fhUnavailable, locCountryCode, lastMatchTime, plus, rankTier,
				leaderboardRank);
	}

	@Override
	protected String getItemTypeDescription() {
		return "player account ID";
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

	private Instant parseDateTime(JsonNode data, String fieldName) {
		if (!data.has(fieldName) || data.get(fieldName).isNull()) {
			return null;
		}

		String dateTimeStr = data.get(fieldName).asText();
		if (dateTimeStr == null || dateTimeStr.trim().isEmpty()) {
			return null;
		}

		try {
			// Try parsing as ISO 8601 string first, as it's more specific.
			return Instant.parse(dateTimeStr);
		}
		catch (java.time.format.DateTimeParseException e) {
			try {
				// Fallback to parsing as a Unix epoch timestamp (number).
				long timestamp = Long.parseLong(dateTimeStr);
				return Instant.ofEpochSecond(timestamp);
			}
			catch (NumberFormatException e2) {
				// All parsing attempts failed.
				System.err.println("Could not parse date time for field: " + fieldName + ", value: " + dateTimeStr);
				return null;
			}
		}
	}

}