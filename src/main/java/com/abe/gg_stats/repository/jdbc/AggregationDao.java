package com.abe.gg_stats.repository.jdbc;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class AggregationDao {

	private final JdbcTemplate jdbcTemplate;

	public void refreshTeamPicksView() {
		jdbcTemplate.execute("REFRESH MATERIALIZED VIEW CONCURRENTLY pro_team_picks_mv");
	}

	public void refreshHeroItemPopularityView() {
		jdbcTemplate.execute("REFRESH MATERIALIZED VIEW CONCURRENTLY pro_hero_item_popularity_mv");
	}

	public void upsertPatches(String jsonArrayLiteral) {
		// jsonArrayLiteral is a JSON array string fetched from /constants/patch
		// Insert or update patch_constants
		String sql = """
				WITH src AS (
				    SELECT (elem->> 'id')::int AS id, elem->> 'name' AS name, (elem->> 'date')::timestamptz AS start_time
				    FROM jsonb_array_elements(?::jsonb) AS elem
				)
				INSERT INTO patch_constants (id, name, start_time)
				SELECT id, name, start_time FROM src
				ON CONFLICT (id) DO UPDATE SET name=EXCLUDED.name, start_time=EXCLUDED.start_time
				""";
		jdbcTemplate.update(sql, jsonArrayLiteral);
	}

	public void aggregateWeeklyHeroTrends() {
		// Patch-level using total team lineups as denominator
		String patchSql = """
				WITH base AS (
				    SELECT m.patch, p.pick_heroes
				    FROM matches m JOIN pro_team_picks_mv p USING(match_id)
				), totals AS (
				    SELECT patch, COUNT(*) AS total_lineups FROM base GROUP BY patch
				), hero_counts AS (
				    SELECT patch, unnest(pick_heroes) AS hero_id, COUNT(*) AS hero_lineups
				    FROM base GROUP BY patch, unnest(pick_heroes)
				)
				INSERT INTO pro_hero_trends (bucket_type, bucket_value, hero_id, matches, picks, pick_rate, win_rate, delta_vs_prev)
				SELECT 'patch', t.patch::text, h.hero_id, t.total_lineups, h.hero_lineups, h.hero_lineups::double precision / t.total_lineups::double precision, NULL, NULL
				FROM totals t JOIN hero_counts h USING(patch)
				ON CONFLICT (bucket_type, bucket_value, hero_id) DO UPDATE SET matches=EXCLUDED.matches, picks=EXCLUDED.picks, pick_rate=EXCLUDED.pick_rate, computed_at=now();
				""";

		// Patch-week bucket: bucket_value = patch-epoch_week
		String patchWeekSql = """
				WITH base AS (
				    SELECT m.patch, floor(m.start_time/604800.0)::bigint AS epoch_week, p.pick_heroes
				    FROM matches m JOIN pro_team_picks_mv p USING(match_id)
				), totals AS (
				    SELECT patch, epoch_week, COUNT(*) AS total_lineups FROM base GROUP BY patch, epoch_week
				), hero_counts AS (
				    SELECT patch, epoch_week, unnest(pick_heroes) AS hero_id, COUNT(*) AS hero_lineups
				    FROM base GROUP BY patch, epoch_week, unnest(pick_heroes)
				), cur AS (
				    SELECT t.patch, t.epoch_week, h.hero_id, t.total_lineups, h.hero_lineups, (h.hero_lineups::double precision / NULLIF(t.total_lineups,0)::double precision) AS pick_rate
				    FROM totals t JOIN hero_counts h USING(patch, epoch_week)
				), prev AS (
				    SELECT patch, (epoch_week - 1) AS epoch_week, hero_id, pick_rate AS prev_pick_rate
				    FROM cur
				)
				INSERT INTO pro_hero_trends (bucket_type, bucket_value, hero_id, matches, picks, pick_rate, win_rate, delta_vs_prev)
				SELECT 'patch_week', (c.patch::text || '-' || c.epoch_week::text), c.hero_id, c.total_lineups, c.hero_lineups, c.pick_rate, NULL, (c.pick_rate - p.prev_pick_rate)
				FROM cur c LEFT JOIN prev p ON p.patch = c.patch AND p.epoch_week = c.epoch_week AND p.hero_id = c.hero_id
				ON CONFLICT (bucket_type, bucket_value, hero_id) DO UPDATE SET matches=EXCLUDED.matches, picks=EXCLUDED.picks, pick_rate=EXCLUDED.pick_rate, delta_vs_prev=EXCLUDED.delta_vs_prev, computed_at=now();
				""";

		jdbcTemplate.execute(patchSql);
		jdbcTemplate.execute(patchWeekSql);
	}

	public void aggregateWeeklyHeroPairs() {
		// Patch-level pairs with support/confidence/lift
		String patchSql = """
				WITH base AS (
				    SELECT m.patch, p.pick_heroes
				    FROM matches m JOIN pro_team_picks_mv p USING(match_id)
				), totals AS (
				    SELECT patch, COUNT(*) AS total_lineups FROM base GROUP BY patch
				), hero_counts AS (
				    SELECT patch, unnest(pick_heroes) AS hero_id, COUNT(*) AS hero_lineups
				    FROM base GROUP BY patch, unnest(pick_heroes)
				), pairs AS (
				    SELECT patch, LEAST(a.hero_id, b.hero_id) AS hero_id_a, GREATEST(a.hero_id, b.hero_id) AS hero_id_b, COUNT(*) AS games_together
				    FROM base
				    CROSS JOIN LATERAL unnest(pick_heroes) WITH ORDINALITY a(hero_id, pos_a)
				    CROSS JOIN LATERAL unnest(pick_heroes) WITH ORDINALITY b(hero_id, pos_b)
				    WHERE b.pos_b > a.pos_a
				    GROUP BY patch, LEAST(a.hero_id, b.hero_id), GREATEST(a.hero_id, b.hero_id)
				)
				INSERT INTO pro_hero_pair_stats (bucket_type, bucket_value, hero_id_a, hero_id_b, games_together, wins_together, support, confidence, lift)
				SELECT 'patch', p.patch::text, p.hero_id_a, p.hero_id_b, p.games_together, NULL::bigint,
				        p.games_together::double precision / t.total_lineups::double precision AS support,
				        p.games_together::double precision / NULLIF(hA.hero_lineups,0)::double precision AS confidence,
				        (p.games_together::double precision / t.total_lineups::double precision) / NULLIF( (hA.hero_lineups::double precision / t.total_lineups::double precision) * (hB.hero_lineups::double precision / t.total_lineups::double precision), 0 ) AS lift
				FROM pairs p
				JOIN totals t USING(patch)
				JOIN hero_counts hA ON hA.patch = p.patch AND hA.hero_id = p.hero_id_a
				JOIN hero_counts hB ON hB.patch = p.patch AND hB.hero_id = p.hero_id_b
				ON CONFLICT (bucket_type, bucket_value, hero_id_a, hero_id_b) DO UPDATE SET games_together=EXCLUDED.games_together, support=EXCLUDED.support, confidence=EXCLUDED.confidence, lift=EXCLUDED.lift, computed_at=now();
				""";

		// Patch-week pairs
		String patchWeekSql = """
				WITH base AS (
				    SELECT m.patch, floor(m.start_time/604800.0)::bigint AS epoch_week, p.pick_heroes
				    FROM matches m JOIN pro_team_picks_mv p USING(match_id)
				), totals AS (
				    SELECT patch, epoch_week, COUNT(*) AS total_lineups FROM base GROUP BY patch, epoch_week
				), hero_counts AS (
				    SELECT patch, epoch_week, unnest(pick_heroes) AS hero_id, COUNT(*) AS hero_lineups
				    FROM base GROUP BY patch, epoch_week, unnest(pick_heroes)
				), pairs AS (
				    SELECT patch, epoch_week, LEAST(a.hero_id, b.hero_id) AS hero_id_a, GREATEST(a.hero_id, b.hero_id) AS hero_id_b, COUNT(*) AS games_together
				    FROM base
				    CROSS JOIN LATERAL unnest(pick_heroes) WITH ORDINALITY a(hero_id, pos_a)
				    CROSS JOIN LATERAL unnest(pick_heroes) WITH ORDINALITY b(hero_id, pos_b)
				    WHERE b.pos_b > a.pos_a
				    GROUP BY patch, epoch_week, LEAST(a.hero_id, b.hero_id), GREATEST(a.hero_id, b.hero_id)
				), cur AS (
				    SELECT p.patch, p.epoch_week, p.hero_id_a, p.hero_id_b, p.games_together,
				           (p.games_together::double precision / NULLIF(t.total_lineups,0)::double precision) AS support,
				           (p.games_together::double precision / NULLIF(hA.hero_lineups,0)::double precision) AS confidence,
				           ((p.games_together::double precision / NULLIF(t.total_lineups,0)::double precision) / NULLIF( (hA.hero_lineups::double precision / NULLIF(t.total_lineups,0)::double precision) * (hB.hero_lineups::double precision / NULLIF(t.total_lineups,0)::double precision), 0 )) AS lift
				    FROM pairs p
				    JOIN totals t USING(patch, epoch_week)
				    JOIN hero_counts hA ON hA.patch = p.patch AND hA.epoch_week = p.epoch_week AND hA.hero_id = p.hero_id_a
				    JOIN hero_counts hB ON hB.patch = p.patch AND hB.epoch_week = p.epoch_week AND hB.hero_id = p.hero_id_b
				), prev AS (
				    SELECT patch, (epoch_week - 1) AS epoch_week, hero_id_a, hero_id_b, support AS prev_support, lift AS prev_lift
				    FROM cur
				)
				INSERT INTO pro_hero_pair_stats (bucket_type, bucket_value, hero_id_a, hero_id_b, games_together, wins_together, support, confidence, lift, delta_support, delta_lift)
				SELECT 'patch_week', (c.patch::text || '-' || c.epoch_week::text), c.hero_id_a, c.hero_id_b, c.games_together, NULL::bigint,
				       c.support, c.confidence, c.lift, (c.support - p.prev_support), (c.lift - p.prev_lift)
				FROM cur c LEFT JOIN prev p ON p.patch = c.patch AND p.epoch_week = c.epoch_week AND p.hero_id_a = c.hero_id_a AND p.hero_id_b = c.hero_id_b
				ON CONFLICT (bucket_type, bucket_value, hero_id_a, hero_id_b) DO UPDATE SET games_together=EXCLUDED.games_together, support=EXCLUDED.support, confidence=EXCLUDED.confidence, lift=EXCLUDED.lift, delta_support=EXCLUDED.delta_support, delta_lift=EXCLUDED.delta_lift, computed_at=now();
				""";

		jdbcTemplate.execute(patchSql);
		jdbcTemplate.execute(patchWeekSql);
	}

}