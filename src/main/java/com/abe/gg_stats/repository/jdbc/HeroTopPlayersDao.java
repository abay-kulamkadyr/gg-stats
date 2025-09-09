package com.abe.gg_stats.repository.jdbc;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class HeroTopPlayersDao {

	private final JdbcTemplate jdbcTemplate;

	public List<Row> topPlayersForHero(int heroId, int limit) {
		String sql = "SELECT hr.account_id, hr.score, p.personname, p.avatarfull FROM hero_ranking hr JOIN player p ON p.account_id = hr.account_id WHERE hr.hero_id=? ORDER BY hr.score DESC NULLS LAST LIMIT ?";
		return jdbcTemplate.query(sql, (rs, i) -> new Row(rs.getLong("account_id"), rs.getDouble("score"),
				rs.getString("personname"), rs.getString("avatarfull")), heroId, limit);
	}

	public record Row(long accountId, double score, String personName, String avatarFull) {
	}

}
