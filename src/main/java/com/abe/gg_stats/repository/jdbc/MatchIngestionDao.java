package com.abe.gg_stats.repository.jdbc;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class MatchIngestionDao {

	private final JdbcTemplate jdbcTemplate;

	public void upsertMatch(JsonNode m) {
		String sql = """
				 INSERT INTO matches (match_id, start_time, duration, pre_game_duration, radiant_win,\s
				     league_id, series_id, series_type, cluster, lobby_type, game_mode, engine, radiant_score, dire_score,\s
				     tower_status_radiant, tower_status_dire, barracks_status_radiant, barracks_status_dire, first_blood_time,\s
				     radiant_team_id, radiant_name, dire_team_id, dire_name, radiant_captain, dire_captain, patch, region, replay_url)\s
				 VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)\s
				 ON CONFLICT (match_id) DO UPDATE SET\s
				     start_time=EXCLUDED.start_time,\s
				     duration=EXCLUDED.duration,\s
				     pre_game_duration=EXCLUDED.pre_game_duration,\s
				     radiant_win=EXCLUDED.radiant_win,\s
				     league_id=EXCLUDED.league_id,\s
				     series_id=EXCLUDED.series_id,\s
				     series_type=EXCLUDED.series_type,\s
				     cluster=EXCLUDED.cluster,\s
				     lobby_type=EXCLUDED.lobby_type,\s
				     game_mode=EXCLUDED.game_mode,\s
				     engine=EXCLUDED.engine,\s
				     radiant_score=EXCLUDED.radiant_score,\s
				     dire_score=EXCLUDED.dire_score,\s
				     tower_status_radiant=EXCLUDED.tower_status_radiant,\s
				     tower_status_dire=EXCLUDED.tower_status_dire,\s
				     barracks_status_radiant=EXCLUDED.barracks_status_radiant,\s
				     barracks_status_dire=EXCLUDED.barracks_status_dire,\s
				     first_blood_time=EXCLUDED.first_blood_time,\s
				     radiant_team_id=EXCLUDED.radiant_team_id,\s
				     radiant_name=EXCLUDED.radiant_name,\s
				     dire_team_id=EXCLUDED.dire_team_id,\s
				     dire_name=EXCLUDED.dire_name,\s
				     radiant_captain=EXCLUDED.radiant_captain,\s
				     dire_captain=EXCLUDED.dire_captain,\s
				     patch=EXCLUDED.patch,\s
				     region=EXCLUDED.region,\s
				     replay_url=EXCLUDED.replay_url,\s
				     updated_at=NOW();
				\s""";

		jdbcTemplate.update(sql, m.path("match_id").asLong(), m.path("start_time").asInt(), m.path("duration").asInt(),
				m.path("pre_game_duration").asInt(0), m.path("radiant_win").asBoolean(), m.path("leagueid").asLong(0),
				m.path("series_id").asLong(0), m.path("series_type").asInt(0), m.path("cluster").asInt(0),
				m.path("lobby_type").asInt(0), m.path("game_mode").asInt(0), m.path("engine").asInt(0),
				m.path("radiant_score").asInt(0), m.path("dire_score").asInt(0),
				m.path("tower_status_radiant").asInt(0), m.path("tower_status_dire").asInt(0),
				m.path("barracks_status_radiant").asInt(0), m.path("barracks_status_dire").asInt(0),
				m.path("first_blood_time").asInt(0), m.path("radiant_team_id").asLong(0),
				m.path("radiant_name").asText(null), m.path("dire_team_id").asLong(0), m.path("dire_name").asText(null),
				m.path("radiant_captain").asLong(0), m.path("dire_captain").asLong(0), m.path("patch").asInt(0),
				m.path("region").asInt(0), m.path("replay_url").asText(null));
	}

	public void upsertTeamMatch(long matchId, long teamId, boolean radiant) {
		String sql = """
				 INSERT INTO team_match (team_id, match_id, radiant)\s
				 VALUES (?,?,?)\s
				 ON CONFLICT (team_id, match_id)\s
				 DO NOTHING
				\s""";
		jdbcTemplate.update(sql, teamId, matchId, radiant);
	}

	public void upsertPickBan(long matchId, int ord, boolean isPick, Integer heroId, int team, Integer playerSlot) {
		String sql = """
				 INSERT INTO picks_bans (match_id, ord, is_pick, hero_id, team, player_slot)\s
				 VALUES (?,?,?,?,?,?)\s
				 ON CONFLICT (match_id, ord) DO UPDATE SET\s
				     is_pick=EXCLUDED.is_pick,\s
				     hero_id=EXCLUDED.hero_id,\s
				     team=EXCLUDED.team,\s
				     player_slot=EXCLUDED.player_slot
				\s""";
		jdbcTemplate.update(sql, matchId, ord, isPick, heroId, team, playerSlot);
	}

	public void upsertDraftTiming(long matchId, int ord, boolean isPick, Integer activeTeam, Integer heroId,
			Integer playerSlot, Integer extraTime, Integer totalTime) {
		String sql = """
				 INSERT INTO draft_timings (match_id, ord, is_pick, active_team, hero_id, player_slot, extra_time, total_time_taken)\s
				 VALUES (?,?,?,?,?,?,?,?)\s
				 ON CONFLICT (match_id, ord) DO UPDATE SET\s
				     is_pick=EXCLUDED.is_pick,\s
				     active_team=EXCLUDED.active_team,\s
				     hero_id=EXCLUDED.hero_id,\s
				     player_slot=EXCLUDED.player_slot,\s
				     extra_time=EXCLUDED.extra_time,\s
				     total_time_taken=EXCLUDED.total_time_taken
				\s""";
		jdbcTemplate.update(sql, matchId, ord, isPick, activeTeam, heroId, playerSlot, extraTime, totalTime);
	}

	public void upsertPlayerMatch(long matchId, long accountId, int playerSlot, int heroId, Boolean win, Integer kills,
			Integer deaths, Integer assists, Integer level, Integer netWorth, Integer gpm, Integer xpm, Integer lane,
			Integer laneRole, Boolean isRoaming) {
		String sql = """
				 INSERT INTO player_matches (match_id, account_id, player_slot, hero_id, win, kills, deaths, assists, level, net_worth, gold_per_min, xp_per_min, lane, lane_role, is_roaming)\s
				 VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)\s
				 ON CONFLICT (match_id, account_id) DO UPDATE SET\s
				     player_slot=EXCLUDED.player_slot,\s
				     hero_id=EXCLUDED.hero_id,\s
				     win=EXCLUDED.win,\s
				     kills=EXCLUDED.kills,\s
				     deaths=EXCLUDED.deaths,\s
				     assists=EXCLUDED.assists,\s
				     level=EXCLUDED.level,\s
				     net_worth=EXCLUDED.net_worth,\s
				     gold_per_min=EXCLUDED.gold_per_min,\s
				     xp_per_min=EXCLUDED.xp_per_min,\s
				     lane=EXCLUDED.lane,\s
				     lane_role=EXCLUDED.lane_role,\s
				     is_roaming=EXCLUDED.is_roaming
				\s""";
		jdbcTemplate.update(sql, matchId, accountId, playerSlot, heroId, win, kills, deaths, assists, level, netWorth,
				gpm, xpm, lane, laneRole, isRoaming);
	}

	public void insertItemPurchaseEvent(long matchId, long accountId, int heroId, int timeS, String itemKey,
			boolean isRadiant, Integer patch) {
		String sql = """
				 INSERT INTO item_purchase_event (match_id, account_id, hero_id, time_s, item_key, is_radiant, patch)\s
				 VALUES (?,?,?,?,?,?,?)\s
				 ON CONFLICT DO NOTHING
				\s""";
		jdbcTemplate.update(sql, matchId, accountId, heroId, timeS, itemKey, isRadiant, patch);
	}

	public Long getMinMatchId() {
		String sql = "SELECT MIN(match_id) FROM matches";
		return jdbcTemplate.queryForObject(sql, Long.class);
	}

	public Long getMaxMatchId() {
		String sql = "SELECT MAX(match_id) FROM matches";
		return jdbcTemplate.queryForObject(sql, Long.class);
	}

	public void refreshProTeamPicksMv() {
		// Simple one-liner, no real benefit to using a text block
		jdbcTemplate.execute("REFRESH MATERIALIZED VIEW CONCURRENTLY pro_team_picks_mv");
	}

}