package com.abe.gg_stats.dto.request.opendota;

import java.time.Instant;

public record OpenDotaPlayerDto(Long accountId, String steamId, String avatar, String avatarMedium, String avatarFull,
		String profileUrl, String personName, Instant lastLogin, Instant fullHistoryTime, Integer cheese,
		Boolean fhUnavailable, String locCountryCode, Instant lastMatchTime, Boolean plus, Integer rankTier,
		Integer leaderboardRank) {

}
