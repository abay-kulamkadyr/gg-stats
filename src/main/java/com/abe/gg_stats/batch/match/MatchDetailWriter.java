package com.abe.gg_stats.batch.match;

import com.abe.gg_stats.batch.BaseWriter;
import com.abe.gg_stats.repository.jdbc.MatchIngestionDao;
import com.abe.gg_stats.util.LoggingConstants;
import com.abe.gg_stats.util.MDCLoggingContext;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MatchDetailWriter extends BaseWriter<JsonNode> {

	private final MatchIngestionDao dao;

	@Override
	protected void writeItem(JsonNode m) {
		MDCLoggingContext.updateContext("operationType", LoggingConstants.OPERATION_TYPE_BATCH);
		MDCLoggingContext.updateContext("batchType", "promatches-detail");

		long matchId = m.path("match_id").asLong();
		// Upsert match core
		dao.upsertMatch(m);

		// team_match rows (if team ids present)
		long radTeam = m.path("radiant_team_id").asLong(0);
		long direTeam = m.path("dire_team_id").asLong(0);
		if (radTeam > 0)
			dao.upsertTeamMatch(matchId, radTeam, true);
		if (direTeam > 0)
			dao.upsertTeamMatch(matchId, direTeam, false);

		// picks_bans
		JsonNode pbArr = m.path("picks_bans");
		if (pbArr.isArray()) {
			int idx = 0;
			for (JsonNode pb : pbArr) {
				dao.upsertPickBan(matchId, pb.path("order").asInt(idx++), pb.path("is_pick").asBoolean(),
						pb.hasNonNull("hero_id") ? pb.get("hero_id").asInt() : null, pb.path("team").asInt(0),
						pb.hasNonNull("player_slot") ? pb.get("player_slot").asInt() : null);
			}
		}

		// draft_timings (optional)
		JsonNode timings = m.path("draft_timings");
		if (timings.isArray()) {
			int idx = 0;
			for (JsonNode dt : timings) {
				dao.upsertDraftTiming(matchId, dt.path("order").asInt(idx++), dt.path("pick").asBoolean(),
						dt.hasNonNull("active_team") ? dt.get("active_team").asInt() : null,
						dt.hasNonNull("hero_id") ? dt.get("hero_id").asInt() : null,
						dt.hasNonNull("player_slot") ? dt.get("player_slot").asInt() : null,
						dt.hasNonNull("extra_time") ? dt.get("extra_time").asInt() : null,
						dt.hasNonNull("total_time_taken") ? dt.get("total_time_taken").asInt() : null);
			}
		}

		// player_matches + item_purchase_event
		int patch = m.path("patch").asInt(0);
		JsonNode players = m.path("players");
		if (players.isArray()) {
			for (JsonNode p : players) {
				long accountId = p.path("account_id").asLong(0);
				int playerSlot = p.path("player_slot").asInt();
				int heroId = p.path("hero_id").asInt();
				Boolean win = p.hasNonNull("win") ? p.get("win").asInt() == 1 : null;
				Integer kills = p.hasNonNull("kills") ? p.get("kills").asInt() : null;
				Integer deaths = p.hasNonNull("deaths") ? p.get("deaths").asInt() : null;
				Integer assists = p.hasNonNull("assists") ? p.get("assists").asInt() : null;
				Integer level = p.hasNonNull("level") ? p.get("level").asInt() : null;
				Integer netWorth = p.hasNonNull("net_worth") ? p.get("net_worth").asInt() : null;
				Integer gpm = p.hasNonNull("gold_per_min") ? p.get("gold_per_min").asInt() : null;
				Integer xpm = p.hasNonNull("xp_per_min") ? p.get("xp_per_min").asInt() : null;
				Integer lane = p.hasNonNull("lane") ? p.get("lane").asInt() : null;
				Integer laneRole = p.hasNonNull("lane_role") ? p.get("lane_role").asInt() : null;
				Boolean roaming = p.hasNonNull("is_roaming") ? p.get("is_roaming").asBoolean() : null;

				dao.upsertPlayerMatch(matchId, accountId, playerSlot, heroId, win, kills, deaths, assists, level,
						netWorth, gpm, xpm, lane, laneRole, roaming);

				// purchases
				JsonNode purchases = p.path("purchase_log");
				boolean isRadiant = playerSlot < 128;
				if (purchases.isArray()) {
					for (JsonNode ev : purchases) {
						int timeS = ev.path("time").asInt();
						String itemKey = ev.path("key").asText(null);
						if (itemKey != null) {
							dao.insertItemPurchaseEvent(matchId, accountId, heroId, timeS, itemKey, isRadiant, patch);
						}
					}
				}
			}
		}

		// Optional: refresh MV after each match (safe but can be heavy). Prefer a
		// periodic refresh.
		// For now, skip per-item refresh; schedule a periodic refresh elsewhere or after
		// chunk.
	}

	@Override
	protected String getItemTypeDescription() {
		return "match-detail";
	}

}
