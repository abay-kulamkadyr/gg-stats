package com.abe.gg_stats.repository.jdbc;

import static org.junit.jupiter.api.Assertions.*;

import com.abe.gg_stats.dto.response.HeroPairsDto;
import com.abe.gg_stats.dto.response.HighlightsHeroDto;
import com.abe.gg_stats.dto.response.HighlightsHeroPairsDto;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@JdbcTest
@ActiveProfiles("test")
@Import(HighlightsDao.class)
class HighlightsDaoTest {

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private HighlightsDao dao;

	@BeforeEach
	void setup() {
		jdbcTemplate.execute("DELETE FROM pro_hero_pair_stats");
		jdbcTemplate.execute("DELETE FROM pro_hero_trends");
		jdbcTemplate.execute("DELETE FROM hero");

		// Seed heroes
		jdbcTemplate.update(
				"INSERT INTO hero (id, created_at, updated_at, attack_type, localized_name, name, primary_attr, roles) VALUES (?,?,?,?,?,?,?,?)",
				1, OffsetDateTime.now(), OffsetDateTime.now(), "Melee", "Hero A", "npc_dota_hero_hero_a", "str",
				"carry");
		jdbcTemplate.update(
				"INSERT INTO hero (id, created_at, updated_at, attack_type, localized_name, name, primary_attr, roles) VALUES (?,?,?,?,?,?,?,?)",
				2, OffsetDateTime.now(), OffsetDateTime.now(), "Ranged", "Hero B", "npc_dota_hero_hero_b", "agi",
				"support");

		// Seed trends across bucket values to test ordering by computed_at
		jdbcTemplate.update(
				"INSERT INTO pro_hero_trends (bucket_type, bucket_value, computed_at, hero_id, matches, picks, pick_rate, delta_vs_prev) VALUES (?,?,?,?,?,?,?,?)",
				"patch", "7.35", OffsetDateTime.now().minusDays(2), 1, 100, 60, 0.6, 0.05);
		jdbcTemplate.update(
				"INSERT INTO pro_hero_trends (bucket_type, bucket_value, computed_at, hero_id, matches, picks, pick_rate, delta_vs_prev) VALUES (?,?,?,?,?,?,?,?)",
				"patch", "7.36", OffsetDateTime.now().minusDays(1), 1, 110, 70, 0.64, null);

		// Seed pairs
		jdbcTemplate.update(
				"INSERT INTO pro_hero_pair_stats (bucket_type, bucket_value, hero_id_a, hero_id_b, games_together, support, confidence, lift, delta_support, delta_lift) VALUES (?,?,?,?,?,?,?,?,?,?)",
				"patch", "7.36", 1, 2, 40, 0.8, 0.9, 1.2, 0.1, 0.2);
	}

	@Test
	void latestBucketValueReturnsMostRecent() {
		String latest = dao.latestBucketValue("patch");
		assertEquals("7.36", latest);
	}

	@Test
	void bucketValueByOffsetRespectsOrdering() {
		String offset0 = dao.bucketValueByOffset("patch", 0);
		String offset1 = dao.bucketValueByOffset("patch", 1);
		assertEquals("7.36", offset0);
		assertEquals("7.35", offset1);
	}

	@Test
	void topHeroesMapsRows() {
		List<HighlightsHeroDto> heroes = dao.topHeroes("patch", "7.36", 10);
		assertFalse(heroes.isEmpty());
		HighlightsHeroDto h = heroes.getFirst();
		assertEquals(1, h.heroId());
		assertEquals(110L, h.matches());
		assertEquals(70L, h.picks());
		assertEquals(0.64, h.pickRate());
	}

	@Test
	void matchesForBucketReturnsMaxMatches() {
		long matches = dao.matchesForBucket("patch", "7.36");
		assertEquals(110L, matches);
	}

	@Test
	void topPairsOrdersAndMaps() {
		List<HeroPairsDto> pairs = dao.topPairs("patch", "7.36", 5, "lift");
		assertFalse(pairs.isEmpty());
		HeroPairsDto p = pairs.get(0);
		assertEquals(1, p.heroIdA());
		assertEquals(2, p.heroIdB());
		assertEquals(40L, p.gamesTogether());
		assertEquals(1.2, p.lift());
	}

	@Test
	void topPairsWithHeroesJoinsHeroData() {
		List<HighlightsHeroPairsDto> pairs = dao.topPairsWithHeroes("patch", "7.36", 5, "support");
		assertFalse(pairs.isEmpty());
		HighlightsHeroPairsDto p = pairs.getFirst();
		assertEquals("Hero A", p.heroALocalizedName());
		assertEquals("npc_dota_hero_hero_a", p.heroAName());
		assertTrue(p.heroAImgUrl().contains("hero_a"));
	}

}
