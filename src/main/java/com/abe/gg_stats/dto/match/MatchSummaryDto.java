package com.abe.gg_stats.dto.match;

public record MatchSummaryDto(long matchId, int startTime, int duration, boolean radiantWin, long leagueId,
		long radiantTeamId, String radiantName, long direTeamId, String direName, int patch, int region) {
}


