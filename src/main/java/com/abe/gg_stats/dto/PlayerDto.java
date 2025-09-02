package com.abe.gg_stats.dto;

import java.time.Instant;

public record PlayerDto(Long accountId, String steamId, String avatar, String avatarMedium, String avatarFull,
		String profileUrl, String personName, Instant lastLogin, Instant fullHistoryTime, Integer cheese,
		Boolean fhUnavailable, String locCountryCode, Instant lastMatchTime, Boolean plus, Integer rankTier,
		Integer leaderboardRank) {
}
