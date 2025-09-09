package com.abe.gg_stats.repository.jdbc;

import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class HighlightsDao {

	private final JdbcTemplate jdbcTemplate;

	public String latestBucketValue(String bucketType) {
		String sql = "SELECT bucket_value FROM pro_hero_trends WHERE bucket_type=? ORDER BY computed_at DESC LIMIT 1";
		List<String> rows = jdbcTemplate.query(sql, (rs, i) -> rs.getString("bucket_value"), bucketType);
		return rows.isEmpty() ? null : rows.get(0);
	}

	public String bucketValueByOffset(String bucketType, int offset) {
		String sql = "SELECT bucket_value FROM pro_hero_trends WHERE bucket_type=? GROUP BY bucket_value ORDER BY MAX(computed_at) DESC OFFSET ? LIMIT 1";
		List<String> rows = jdbcTemplate.query(sql, (rs, i) -> rs.getString("bucket_value"), bucketType, offset);
		return rows.isEmpty() ? null : rows.get(0);
	}

	public List<HeroRow> topHeroes(String bucketType, String bucketValue, int limit) {
		String sql = "SELECT hero_id, matches, picks, pick_rate, delta_vs_prev FROM pro_hero_trends WHERE bucket_type=? AND bucket_value=? ORDER BY pick_rate DESC, picks DESC LIMIT ?";
		return jdbcTemplate.query(sql,
				(rs, i) -> new HeroRow(rs.getInt("hero_id"), rs.getLong("matches"), rs.getLong("picks"),
						rs.getDouble("pick_rate"),
						rs.getObject("delta_vs_prev") == null ? null : rs.getDouble("delta_vs_prev")),
				bucketType, bucketValue, limit);
	}

	public long matchesForBucket(String bucketType, String bucketValue) {
		String sql = "SELECT MAX(matches) FROM pro_hero_trends WHERE bucket_type=? AND bucket_value=?";
		Long v = jdbcTemplate.queryForObject(sql, Long.class, bucketType, bucketValue);
		return v == null ? 0L : v;
	}

	public List<PairRow> topPairs(String bucketType, String bucketValue, int limit, String sort) {
		String orderBy;
		String normalized = (sort == null ? "lift" : sort.toLowerCase());
		switch (normalized) {
			case "support":
				orderBy = "support DESC, games_together DESC";
				break;
			case "confidence":
				orderBy = "confidence DESC, games_together DESC";
				break;
			case "delta_lift":
				orderBy = "delta_lift DESC NULLS LAST, lift DESC";
				break;
			case "delta_support":
				orderBy = "delta_support DESC NULLS LAST, support DESC";
				break;
			case "games":
				orderBy = "games_together DESC";
				break;
			case "lift":
			default:
				orderBy = "lift DESC, support DESC";
				break;
		}
		String sql = "SELECT hero_id_a, hero_id_b, games_together, support, confidence, lift, delta_support, delta_lift "
				+ "FROM pro_hero_pair_stats WHERE bucket_type=? AND bucket_value=? " + "ORDER BY " + orderBy
				+ " LIMIT ?";
		return jdbcTemplate.query(sql,
				(rs, i) -> new PairRow(rs.getInt("hero_id_a"), rs.getInt("hero_id_b"), rs.getLong("games_together"),
						rs.getDouble("support"), rs.getDouble("confidence"), rs.getDouble("lift"),
						rs.getObject("delta_support") == null ? null : rs.getDouble("delta_support"),
						rs.getObject("delta_lift") == null ? null : rs.getDouble("delta_lift")),
				bucketType, bucketValue, limit);
	}

	public List<PairRow> topPairs(String bucketType, String bucketValue, int limit) {
		return topPairs(bucketType, bucketValue, limit, "lift");
	}

	public record HeroRow(int heroId, long matches, long picks, double pickRate, Double deltaVsPrev) {
	}

	public record PairRow(int heroIdA, int heroIdB, long gamesTogether, double support, double confidence, double lift,
			Double deltaSupport, Double deltaLift) {
	}

	public List<PairWithHeroRow> topPairsWithHeroes(String bucketType, String bucketValue, int limit, String sort) {
		String orderBy;
		String normalized = (sort == null ? "lift" : sort.toLowerCase());
		switch (normalized) {
			case "support":
				orderBy = "p.support DESC, p.games_together DESC";
				break;
			case "confidence":
				orderBy = "p.confidence DESC, p.games_together DESC";
				break;
			case "delta_lift":
				orderBy = "p.delta_lift DESC NULLS LAST, p.lift DESC";
				break;
			case "delta_support":
				orderBy = "p.delta_support DESC NULLS LAST, p.support DESC";
				break;
			case "games":
				orderBy = "p.games_together DESC";
				break;
			case "lift":
			default:
				orderBy = "p.lift DESC, p.support DESC";
				break;
		}
		String sql = "SELECT p.hero_id_a, p.hero_id_b, p.games_together, p.support, p.confidence, p.lift, p.delta_support, p.delta_lift, "
				+ "ha.localized_name AS hero_a_localized_name, hb.localized_name AS hero_b_localized_name, "
				+ "ha.name AS hero_a_name, hb.name AS hero_b_name, "
				+ "LOWER(REPLACE(ha.name, 'npc_dota_hero_', '')) AS hero_a_cdn_name, "
				+ "LOWER(REPLACE(hb.name, 'npc_dota_hero_', '')) AS hero_b_cdn_name, "
				+ "'https://cdn.steamstatic.com/apps/dota2/images/dota_react/heroes/' || LOWER(REPLACE(ha.name, 'npc_dota_hero_', '')) || '.png' AS hero_a_img_url, "
				+ "'https://cdn.steamstatic.com/apps/dota2/images/dota_react/heroes/' || LOWER(REPLACE(hb.name, 'npc_dota_hero_', '')) || '.png' AS hero_b_img_url "
				+ "FROM pro_hero_pair_stats p " + "JOIN hero ha ON ha.id = p.hero_id_a "
				+ "JOIN hero hb ON hb.id = p.hero_id_b " + "WHERE p.bucket_type=? AND p.bucket_value=? " + "ORDER BY "
				+ orderBy + " LIMIT ?";
		return jdbcTemplate.query(sql, (rs, i) -> new PairWithHeroRow(rs.getInt("hero_id_a"), rs.getInt("hero_id_b"),
				rs.getLong("games_together"), rs.getDouble("support"), rs.getDouble("confidence"), rs.getDouble("lift"),
				rs.getObject("delta_support") == null ? null : rs.getDouble("delta_support"),
				rs.getObject("delta_lift") == null ? null : rs.getDouble("delta_lift"),
				rs.getString("hero_a_localized_name"), rs.getString("hero_b_localized_name"),
				rs.getString("hero_a_name"), rs.getString("hero_b_name"), rs.getString("hero_a_cdn_name"),
				rs.getString("hero_b_cdn_name"), rs.getString("hero_a_img_url"), rs.getString("hero_b_img_url")),
				bucketType, bucketValue, limit);
	}

	public record PairWithHeroRow(int heroIdA, int heroIdB, long gamesTogether, double support, double confidence,
			double lift, Double deltaSupport, Double deltaLift, String heroALocalizedName, String heroBLocalizedName,
			String heroAName, String heroBName, String heroACdnName, String heroBCdnName, String heroAImgUrl,
			String heroBImgUrl) {
	}

}
