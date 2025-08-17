package com.abe.gg_stats.batch;

import com.abe.gg_stats.batch.heroRanking.HeroRankingProcessor;
import com.abe.gg_stats.entity.HeroRanking;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

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
	void testProcess_ValidHeroRankingData_ShouldCreateList() throws Exception {
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
	void testProcess_ValidDataWithoutScore_ShouldHandleGracefully() throws Exception {
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
	void testProcess_EmptyRankingsArray_ShouldReturnEmptyList() throws Exception {
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
	void testProcess_MissingHeroId_ShouldReturnNull() throws Exception {
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
	void testProcess_MissingRankings_ShouldReturnNull() throws Exception {
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
	void testProcess_NullHeroId_ShouldReturnNull() throws Exception {
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
	void testProcess_RankingsNotArray_ShouldReturnNull() throws Exception {
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
	void testProcess_InvalidAccountId_ShouldFilterOutInvalidRankings() throws Exception {
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
							"account_id": -1,
							"score": 88.0
						},
						{
							"account_id": 67890,
							"score": 92.0
						}
					]
				}
				""";
		JsonNode item = objectMapper.readTree(validJson);

		// When
		List<HeroRanking> result = processor.process(item);

		// Then
		assertNotNull(result);
		assertEquals(2, result.size()); // Should filter out the negative account_id

		// Verify valid rankings are included
		assertTrue(result.stream().anyMatch(r -> r.getAccountId() == 12345L));
		assertTrue(result.stream().anyMatch(r -> r.getAccountId() == 67890L));
		// Verify invalid ranking is filtered out
		assertFalse(result.stream().anyMatch(r -> r.getAccountId() == -1L));
	}

	@Test
	void testProcess_WithNullValuesInRankings_ShouldFilterOutNulls() throws Exception {
		// Given
		String validJson = """
				{
					"hero_id": 1,
					"rankings": [
						{
							"account_id": 12345,
							"score": 95.5
						},
						null,
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
		assertEquals(2, result.size()); // Should filter out the null ranking
	}

	@Test
	void testProcess_WithExceptionInRanking_ShouldFilterOutFailedRankings() throws Exception {
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
							"account_id": "invalid_id",
							"score": 88.0
						},
						{
							"account_id": 67890,
							"score": 92.0
						}
					]
				}
				""";
		JsonNode item = objectMapper.readTree(validJson);

		// When
		List<HeroRanking> result = processor.process(item);

		// Then
		assertNotNull(result);
		assertEquals(2, result.size()); // Should filter out the invalid ranking

		// Verify valid rankings are included
		assertTrue(result.stream().anyMatch(r -> r.getAccountId() == 12345L));
		assertTrue(result.stream().anyMatch(r -> r.getAccountId() == 67890L));
	}

	@Test
	void testProcess_WithMaxValues_ShouldHandleCorrectly() throws Exception {
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
	void testProcess_WithDifferentScoreTypes_ShouldHandleCorrectly() throws Exception {
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
