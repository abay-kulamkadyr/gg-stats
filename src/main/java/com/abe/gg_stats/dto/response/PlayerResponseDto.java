package com.abe.gg_stats.dto.response;

public record PlayerResponseDto(Long accountId, ProfileDto profile, Integer rankTier, Integer leaderboardRank) {

	public record ProfileDto(Long accountId, String steamid, String avatar, String avatarmedium, String avatarfull,
			String profileurl, String personaname, String last_login, String full_history_time, String last_match_time,
			Integer cheese, Boolean fh_unavailable, String loccountrycode, Boolean plus) {
	}
}



