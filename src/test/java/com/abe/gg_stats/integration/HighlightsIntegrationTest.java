package com.abe.gg_stats.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.abe.gg_stats.dto.response.HighlightsDuoDto;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class HighlightsIntegrationTest {

	@LocalServerPort
	private int port;

	@Autowired
	private TestRestTemplate restTemplate;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private ObjectMapper objectMapper;

	private String baseUrl() {
		return "http://localhost:" + port;
	}

	@BeforeEach
	void setupData() {
		// Clean and seed minimal data
		jdbcTemplate.execute("DELETE FROM pro_hero_pair_stats");
		jdbcTemplate.execute("DELETE FROM pro_hero_trends");
		jdbcTemplate.execute("DELETE FROM hero");

		// Seed heroes referenced by pair stats
		jdbcTemplate.update(
				"INSERT INTO hero (id, created_at, updated_at, attack_type, localized_name, name, primary_attr, roles) VALUES (?,?,?,?,?,?,?,?)",
				1, OffsetDateTime.now(), OffsetDateTime.now(), "Melee", "Hero A", "npc_dota_hero_hero_a", "str",
				"carry");
		jdbcTemplate.update(
				"INSERT INTO hero (id, created_at, updated_at, attack_type, localized_name, name, primary_attr, roles) VALUES (?,?,?,?,?,?,?,?)",
				2, OffsetDateTime.now(), OffsetDateTime.now(), "Ranged", "Hero B", "npc_dota_hero_hero_b", "agi",
				"support");

		// Seed trends for bucket 'patch' and value '7.36'
		jdbcTemplate.update(
				"INSERT INTO pro_hero_trends (bucket_type, bucket_value, computed_at, hero_id, matches, picks, pick_rate, delta_vs_prev) VALUES (?,?,?,?,?,?,?,?)",
				"patch", "7.36", OffsetDateTime.now(), 1, 100, 60, 0.6, 0.05);
		jdbcTemplate.update(
				"INSERT INTO pro_hero_trends (bucket_type, bucket_value, computed_at, hero_id, matches, picks, pick_rate, delta_vs_prev) VALUES (?,?,?,?,?,?,?,?)",
				"patch", "7.36", OffsetDateTime.now(), 2, 100, 50, 0.5, null);

		// Seed pair stats for bucket_type 'patch_week' with bucket_value '2025-W37'
		// Also seed a corresponding trend row so latestBucketValue can resolve
		jdbcTemplate.update(
				"INSERT INTO pro_hero_trends (bucket_type, bucket_value, computed_at, hero_id, matches, picks, pick_rate, delta_vs_prev) VALUES (?,?,?,?,?,?,?,?)",
				"patch_week", "2025-W37", OffsetDateTime.now(), 1, 100, 60, 0.6, 0.05);
		jdbcTemplate.update(
				"INSERT INTO pro_hero_pair_stats (bucket_type, bucket_value, hero_id_a, hero_id_b, games_together, support, confidence, lift, delta_support, delta_lift) VALUES (?,?,?,?,?,?,?,?,?,?)",
				"patch_week", "2025-W37", 1, 2, 40, 0.8, 0.9, 1.2, 0.1, 0.2);
	}

	@Test
	void highlightsEndpointReturnsData() {
		ResponseEntity<String> resp = restTemplate.getForEntity(baseUrl() + "/highlights?bucket=patch&value=7.36",
				String.class);
		assertEquals(HttpStatus.OK, resp.getStatusCode());
		assertNotNull(resp.getBody());
		assertTrue(resp.getBody().contains("matches"));
	}

	@Test
	void pairHighlightsEndpointReturnsData() {
		ResponseEntity<String> resp = restTemplate
			.getForEntity(baseUrl() + "/highlights/pairs?view=synergy&weekOffset=0&limit=10", String.class);
		assertEquals(HttpStatus.OK, resp.getStatusCode());
		assertNotNull(resp.getBody());
		try {
			HighlightsDuoDto dto = objectMapper.readValue(resp.getBody(), HighlightsDuoDto.class);
			assertNotNull(dto);
			assertNotNull(dto.pairs());
			assertFalse(dto.pairs().isEmpty());
			assertEquals("Hero A", dto.pairs().getFirst().heroALocalizedName());
		}
		catch (Exception e) {
			throw new AssertionError("Failed to parse response: " + e.getMessage(), e);
		}
	}

}
