package com.abe.gg_stats.batch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.abe.gg_stats.batch.hero_ranking.HeroRankingProcessor;
import com.abe.gg_stats.config.JacksonConfig;
import com.abe.gg_stats.dto.request.opendota.OpenDotaHeroRankingDto;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

class HeroRankingProcessorTest {

	private HeroRankingProcessor processor;

	private ObjectMapper objectMapper;

	@BeforeEach
	void setUp() {
		processor = new HeroRankingProcessor();
		objectMapper = new JacksonConfig().objectMapper();
	}

	@Test
	void testProcess_ValidHeroRankingData_ShouldCreateList() throws JsonProcessingException {
		String validJson = """
				{
					"hero_id": 1,
					"rankings": [
						{
							"account_id": 12345,
							"score": 95.5
						},
						{
							"account_id": 67890,
							"score": 88.0
						}
					]
				}
				""";
		JsonNode item = objectMapper.readTree(validJson);

		List<OpenDotaHeroRankingDto> result = processor.process(item);

		assertNotNull(result);
		assertEquals(2, result.size());

		OpenDotaHeroRankingDto firstRanking = result.getFirst();
		assertEquals(1, firstRanking.heroId());
		assertEquals(12345L, firstRanking.accountId());
		assertEquals(95.5, firstRanking.score());

		OpenDotaHeroRankingDto secondRanking = result.get(1);
		assertEquals(1, secondRanking.heroId());
		assertEquals(67890L, secondRanking.accountId());
		assertEquals(88.0, secondRanking.score());
	}

	@Test
	void testProcess_ValidDataWithoutScore_ShouldHandleGracefully() throws JsonProcessingException {
		String validJson = """
				{
					"hero_id": 2,
					"rankings": [
						{
							"account_id": 12345
						}
					]
				}
				""";
		JsonNode item = objectMapper.readTree(validJson);

		List<OpenDotaHeroRankingDto> result = processor.process(item);

		assertNotNull(result);
		assertEquals(1, result.size());

		OpenDotaHeroRankingDto ranking = result.get(0);
		assertEquals(2, ranking.heroId());
		assertEquals(12345L, ranking.accountId());
		assertNull(ranking.score());
	}

	@Test
	void testProcess_EmptyRankingsArray_ShouldReturnEmptyList() throws JsonProcessingException {
		String validJson = """
				{
					"hero_id": 3,
					"rankings": []
				}
				""";
		JsonNode item = objectMapper.readTree(validJson);

		List<OpenDotaHeroRankingDto> result = processor.process(item);

		assertNotNull(result);
		assertTrue(result.isEmpty());
	}

	@Test
	void testProcess_MissingHeroId_ShouldReturnNull() throws JsonProcessingException {
		String invalidJson = """
				{
					"rankings": [
						{
							"account_id": 12345,
							"score": 95.5
						}
					]
				}
				""";
		JsonNode item = objectMapper.readTree(invalidJson);

		List<OpenDotaHeroRankingDto> result = processor.process(item);

		assertNull(result);
	}

	@Test
	void testProcess_MissingRankings_ShouldReturnNull() throws JsonProcessingException {
		String invalidJson = """
				{
					"hero_id": 1
				}
				""";
		JsonNode item = objectMapper.readTree(invalidJson);

		List<OpenDotaHeroRankingDto> result = processor.process(item);

		assertNull(result);
	}

	@Test
	void testProcess_NullHeroId_ShouldReturnNull() throws JsonProcessingException {
		String invalidJson = """
				{
					"hero_id": null,
					"rankings": [
						{
							"account_id": 12345,
							"score": 95.5
						}
					]
				}
				""";
		JsonNode item = objectMapper.readTree(invalidJson);

		List<OpenDotaHeroRankingDto> result = processor.process(item);

		assertNull(result);
	}

	@Test
	void testProcess_RankingsNotArray_ShouldReturnNull() throws JsonProcessingException {
		String invalidJson = """
				{
					"hero_id": 1,
					"rankings": "not_an_array"
				}
				""";
		JsonNode item = objectMapper.readTree(invalidJson);

		List<OpenDotaHeroRankingDto> result = processor.process(item);

		assertNull(result);
	}

	@Test
	void testProcess_WithMaxValues_ShouldHandleCorrectly() throws JsonProcessingException {
		String validJson = """
				{
					"hero_id": 2147483647,
					"rankings": [
						{
							"account_id": 9223372036854775807,
							"score": 100.0
						}
					]
				}
				""";
		JsonNode item = objectMapper.readTree(validJson);

		List<OpenDotaHeroRankingDto> result = processor.process(item);

		assertNotNull(result);
		assertEquals(1, result.size());

		OpenDotaHeroRankingDto ranking = result.get(0);
		assertEquals(2147483647, ranking.heroId());
		assertEquals(9223372036854775807L, ranking.accountId());
		assertEquals(100.0, ranking.score());
	}

	@Test
	void testProcess_WithDifferentScoreTypes_ShouldHandleCorrectly() throws JsonProcessingException {
		String validJson = """
				{
					"hero_id": 1,
					"rankings": [
						{
							"account_id": 12345,
							"score": 100
						}
					]
				}
				""";
		JsonNode item = objectMapper.readTree(validJson);

		List<OpenDotaHeroRankingDto> result = processor.process(item);

		assertNotNull(result);
		assertEquals(1, result.size());
		assertEquals(100.0, result.get(0).score());
	}

}
