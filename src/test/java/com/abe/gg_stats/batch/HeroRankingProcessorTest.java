package com.abe.gg_stats.batch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.abe.gg_stats.batch.heroRanking.HeroRankingProcessor;
import com.abe.gg_stats.entity.HeroRanking;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

@ExtendWith(MockitoExtension.class)
class HeroRankingProcessorTest {

	private HeroRankingProcessor processor;

	private ObjectMapper objectMapper;

	@BeforeEach
	void setUp() {
		processor = new HeroRankingProcessor();
		objectMapper = new ObjectMapper();
	}

	@Test
	void testProcess_ValidHeroRankingData_ShouldCreateList() throws JsonProcessingException {
		// Given
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

		// When
		List<HeroRanking> result = processor.process(item);

		// Then
		assertNotNull(result);
		assertEquals(2, result.size());

		HeroRanking firstRanking = result.get(0);
		assertEquals(1, firstRanking.getHeroId());
		assertEquals(12345L, firstRanking.getAccountId());
		assertEquals(95.5, firstRanking.getScore());

		HeroRanking secondRanking = result.get(1);
		assertEquals(1, secondRanking.getHeroId());
		assertEquals(67890L, secondRanking.getAccountId());
		assertEquals(88.0, secondRanking.getScore());
	}

	@Test
	void testProcess_ValidDataWithoutScore_ShouldHandleGracefully() throws JsonProcessingException {
		// Given
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

		// When
		List<HeroRanking> result = processor.process(item);

		// Then
		assertNotNull(result);
		assertEquals(1, result.size());

		HeroRanking ranking = result.get(0);
		assertEquals(2, ranking.getHeroId());
		assertEquals(12345L, ranking.getAccountId());
		assertNull(ranking.getScore());
	}

	@Test
	void testProcess_EmptyRankingsArray_ShouldReturnEmptyList() throws JsonProcessingException {
		// Given
		String validJson = """
				{
					"hero_id": 3,
					"rankings": []
				}
				""";
		JsonNode item = objectMapper.readTree(validJson);

		// When
		List<HeroRanking> result = processor.process(item);

		// Then
		assertNotNull(result);
		assertTrue(result.isEmpty());
	}

	@Test
	void testProcess_MissingHeroId_ShouldReturnNull() throws JsonProcessingException {
		// Given
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

		// When
		List<HeroRanking> result = processor.process(item);

		// Then
		assertNull(result);
	}

	@Test
	void testProcess_MissingRankings_ShouldReturnNull() throws JsonProcessingException {
		// Given
		String invalidJson = """
				{
					"hero_id": 1
				}
				""";
		JsonNode item = objectMapper.readTree(invalidJson);

		// When
		List<HeroRanking> result = processor.process(item);

		// Then
		assertNull(result);
	}

	@Test
	void testProcess_NullHeroId_ShouldReturnNull() throws JsonProcessingException {
		// Given
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

		// When
		List<HeroRanking> result = processor.process(item);

		// Then
		assertNull(result);
	}

	@Test
	void testProcess_RankingsNotArray_ShouldReturnNull() throws JsonProcessingException {
		// Given
		String invalidJson = """
				{
					"hero_id": 1,
					"rankings": "not_an_array"
				}
				""";
		JsonNode item = objectMapper.readTree(invalidJson);

		// When
		List<HeroRanking> result = processor.process(item);

		// Then
		assertNull(result);
	}

	@Test
	void testProcess_WithMaxValues_ShouldHandleCorrectly() throws JsonProcessingException {
		// Given
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

		// When
		List<HeroRanking> result = processor.process(item);

		// Then
		assertNotNull(result);
		assertEquals(1, result.size());

		HeroRanking ranking = result.get(0);
		assertEquals(2147483647, ranking.getHeroId());
		assertEquals(9223372036854775807L, ranking.getAccountId());
		assertEquals(100.0, ranking.getScore());
	}

	@Test
	void testProcess_WithDifferentScoreTypes_ShouldHandleCorrectly() throws JsonProcessingException {
		// Given
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

		// When
		List<HeroRanking> result = processor.process(item);

		// Then
		assertNotNull(result);
		assertEquals(1, result.size());
		assertEquals(100.0, result.get(0).getScore());
	}

}
