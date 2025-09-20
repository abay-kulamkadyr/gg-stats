package com.abe.gg_stats.repository.jdbc;

import com.abe.gg_stats.dto.response.HeroItemsCountDto;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class HeroItemsDao {

	private final JdbcTemplate jdbcTemplate;

	public List<HeroItemsCountDto> topItemsForHero(int heroId, String timeBucket, int limit) {
		String sql = "SELECT item_key, purchases FROM pro_hero_item_popularity_mv WHERE hero_id=? AND time_bucket=? ORDER BY purchases DESC, item_key ASC LIMIT ?";
		return jdbcTemplate.query(sql,
				(rs, i) -> new HeroItemsCountDto(rs.getString("item_key"), rs.getLong("purchases")), heroId, timeBucket,
				limit);
	}

}
