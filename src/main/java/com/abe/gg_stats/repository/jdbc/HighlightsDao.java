package com.abe.gg_stats.repository.jdbc;

import com.abe.gg_stats.dto.response.HeroPairsDto;
import com.abe.gg_stats.dto.response.HighlightsDto;
import com.abe.gg_stats.dto.response.HighlightsHeroDto;
import com.abe.gg_stats.dto.response.HighlightsHeroPairsDto;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import lombok.RequiredArgsConstructor;

@Repository
public class HighlightsDao {

	private final JdbcTemplate jdbcTemplate;

	@Autowired
	public HighlightsDao(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public String latestBucketValue(String bucketType) {
		String sql = "SELECT bucket_value FROM pro_hero_trends WHERE bucket_type=? ORDER BY computed_at DESC LIMIT 1";
		List<String> rows = jdbcTemplate.query(sql, (rs, i) -> rs.getString("bucket_value"), bucketType);
		return rows.isEmpty() ? null : rows.getFirst();
	}

	public String bucketValueByOffset(String bucketType, int offset) {
		String sql = "SELECT bucket_value FROM pro_hero_trends WHERE bucket_type=? GROUP BY bucket_value ORDER BY MAX(computed_at) DESC OFFSET ? LIMIT 1";
		List<String> rows = jdbcTemplate.query(sql, (rs, i) -> rs.getString("bucket_value"), bucketType, offset);
		return rows.isEmpty() ? null : rows.getFirst();
	}

	public List<HighlightsHeroDto> topHeroes(String bucketType, String bucketValue, int limit) {
		String sql = "SELECT hero_id, matches, picks, pick_rate, delta_vs_prev FROM pro_hero_trends WHERE bucket_type=? AND bucket_value=? ORDER BY pick_rate DESC, picks DESC LIMIT ?";
		return jdbcTemplate.query(sql,
				(rs, i) -> new HighlightsHeroDto(rs.getInt("hero_id"), rs.getLong("matches"), rs.getLong("picks"),
						rs.getDouble("pick_rate"),
						rs.getObject("delta_vs_prev") == null ? null : rs.getDouble("delta_vs_prev")),
				bucketType, bucketValue, limit);
	}

	public long matchesForBucket(String bucketType, String bucketValue) {
		String sql = "SELECT MAX(matches) FROM pro_hero_trends WHERE bucket_type=? AND bucket_value=?";
		Long v = jdbcTemplate.queryForObject(sql, Long.class, bucketType, bucketValue);
		return v == null ? 0L : v;
	}

	public List<HeroPairsDto> topPairs(String bucketType, String bucketValue, int limit, String sort) {
		String orderBy;
		String normalized = (sort == null ? "lift" : sort.toLowerCase());
		orderBy = switch (normalized) {
			case "support" -> "support DESC, games_together DESC";
			case "confidence" -> "confidence DESC, games_together DESC";
			case "delta_lift" -> "delta_lift DESC NULLS LAST, lift DESC";
			case "delta_support" -> "delta_support DESC NULLS LAST, support DESC";
			case "games" -> "games_together DESC";
			default -> "lift DESC, support DESC";
		};
		String sql = "SELECT hero_id_a, hero_id_b, games_together, support, confidence, lift, delta_support, delta_lift "
				+ "FROM pro_hero_pair_stats WHERE bucket_type=? AND bucket_value=? " + "ORDER BY " + orderBy
				+ " LIMIT ?";
		return jdbcTemplate.query(sql, (rs, i) -> new HeroPairsDto(rs.getInt("hero_id_a"), rs.getInt("hero_id_b"),
				rs.getLong("games_together"), rs.getDouble("support"), rs.getDouble("confidence"), rs.getDouble("lift"),
				rs.getObject("delta_support") == null ? null : rs.getDouble("delta_support"),
				rs.getObject("delta_lift") == null ? null : rs.getDouble("delta_lift")), bucketType, bucketValue,
				limit);
	}

	public List<HighlightsHeroPairsDto> topPairsWithHeroes(String bucketType, String bucketValue, int limit,
			String sort) {
		String orderBy = getOrderCriteria(sort);

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

		return jdbcTemplate
			.query(sql, (rs, i) -> new HighlightsHeroPairsDto(rs.getInt("hero_id_a"), rs.getInt("hero_id_b"),
					rs.getLong("games_together"), rs.getDouble("support"), rs.getDouble("confidence"),
					rs.getDouble("lift"), rs.getObject("delta_support") == null ? null : rs.getDouble("delta_support"),
					rs.getObject("delta_lift") == null ? null : rs.getDouble("delta_lift"),
					rs.getString("hero_a_localized_name"), rs.getString("hero_b_localized_name"),
					rs.getString("hero_a_name"), rs.getString("hero_b_name"), rs.getString("hero_a_cdn_name"),
					rs.getString("hero_b_cdn_name"), rs.getString("hero_a_img_url"), rs.getString("hero_b_img_url")),
					bucketType, bucketValue, limit);
	}

	private String getOrderCriteria(String sort) {
		String normalized = (sort == null ? "lift" : sort.toLowerCase());
		return switch (normalized) {
			case "support" -> "p.support DESC, p.games_together DESC";
			case "confidence" -> "p.confidence DESC, p.games_together DESC";
			case "delta_lift" -> "p.delta_lift DESC NULLS LAST, p.lift DESC";
			case "delta_support" -> "p.delta_support DESC NULLS LAST, p.support DESC";
			case "games" -> "p.games_together DESC";
			default -> "p.lift DESC, p.support DESC";
		};
	}

}
