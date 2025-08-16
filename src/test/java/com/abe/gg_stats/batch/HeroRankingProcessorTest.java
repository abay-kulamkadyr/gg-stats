package com.abe.gg_stats.batch;

import com.abe.gg_stats.batch.heroRanking.HeroRankingProcessor;
import com.abe.gg_stats.entity.HeroRanking;
import com.abe.gg_stats.repository.HeroRankingRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HeroRankingProcessorTest {

	@Mock
	private HeroRankingRepository heroRankingRepository;

	private HeroRankingProcessor processor;

	private ObjectMapper objectMapper;

	@BeforeEach
	void setUp() {
		processor = new HeroRankingProcessor(heroRankingRepository);
		objectMapper = new ObjectMapper();
	}

	@Test
	void testProcess_ValidNewHeroRanking_ShouldCreateNew() throws Exception {
		// Given
		String validJson = """
				{
					"account_id": 12345,
					"hero_id": 1,
					"score": 95.5
				}
				""";
		JsonNode item = objectMapper.readTree(validJson);

		when(heroRankingRepository.findByHeroIdAndAccountId(12345L, 1)).thenReturn(Optional.empty());

		// When
		HeroRanking result = processor.process(item);

		// Then
		assertNotNull(result);
		assertEquals(12345L, result.getAccountId());
		assertEquals(1, result.getHeroId());
		assertEquals(95.5, result.getScore());

		verify(heroRankingRepository).findByHeroIdAndAccountId(12345L, 1);
	}

	@Test
	void testProcess_ValidExistingHeroRanking_ShouldUpdate() throws Exception {
		// Given
		String validJson = """
				{
					"account_id": 12345,
					"hero_id": 1,
					"score": 98.0
				}
				""";
		JsonNode item = objectMapper.readTree(validJson);

		HeroRanking existingRanking = new HeroRanking();
		existingRanking.setAccountId(12345L);
		existingRanking.setHeroId(1);
		existingRanking.setScore(90.0);

		when(heroRankingRepository.findByHeroIdAndAccountId(12345L, 1)).thenReturn(Optional.of(existingRanking));

		// When
		HeroRanking result = processor.process(item);

		// Then
		assertNotNull(result);
		assertEquals(12345L, result.getAccountId());
		assertEquals(1, result.getHeroId());
		assertEquals(98.0, result.getScore());
		assertSame(existingRanking, result); // Should be the same instance

		verify(heroRankingRepository).findByHeroIdAndAccountId(12345L, 1);
	}

	@Test
	void testProcess_WithoutScore_ShouldHandleGracefully() throws Exception {
		// Given
		String validJson = """
				{
					"account_id": 12345,
					"hero_id": 1
				}
				""";
		JsonNode item = objectMapper.readTree(validJson);

		when(heroRankingRepository.findByHeroIdAndAccountId(12345L, 1)).thenReturn(Optional.empty());

		// When
		HeroRanking result = processor.process(item);

		// Then
		assertNotNull(result);
		assertEquals(12345L, result.getAccountId());
		assertEquals(1, result.getHeroId());
		assertNull(result.getScore());
	}

	@Test
	void testProcess_NullItem_ShouldReturnNull() throws Exception {
		// When
		HeroRanking result = processor.process(null);

		// Then
		assertNull(result);
		verify(heroRankingRepository, never()).findByHeroIdAndAccountId(any(), any());
	}

	@Test
	void testProcess_MissingAccountId_ShouldReturnNull() throws Exception {
		// Given
		String invalidJson = """
				{
					"hero_id": 1,
					"score": 95.5
				}
				""";
		JsonNode item = objectMapper.readTree(invalidJson);

		// When
		HeroRanking result = processor.process(item);

		// Then
		assertNull(result);
		verify(heroRankingRepository, never()).findByHeroIdAndAccountId(any(), any());
	}

	@Test
	void testProcess_MissingHeroId_ShouldReturnNull() throws Exception {
		// Given
		String invalidJson = """
				{
					"account_id": 12345,
					"score": 95.5
				}
				""";
		JsonNode item = objectMapper.readTree(invalidJson);

		// When
		HeroRanking result = processor.process(item);

		// Then
		assertNull(result);
		verify(heroRankingRepository, never()).findByHeroIdAndAccountId(any(), any());
	}

	@Test
	void testProcess_NullAccountId_ShouldReturnNull() throws Exception {
		// Given
		String invalidJson = """
				{
					"account_id": null,
					"hero_id": 1,
					"score": 95.5
				}
				""";
		JsonNode item = objectMapper.readTree(invalidJson);

		// When
		HeroRanking result = processor.process(item);

		// Then
		assertNull(result);
		verify(heroRankingRepository, never()).findByHeroIdAndAccountId(any(), any());
	}

	@Test
	void testProcess_NullHeroId_ShouldReturnNull() throws Exception {
		// Given
		String invalidJson = """
				{
					"account_id": 12345,
					"hero_id": null,
					"score": 95.5
				}
				""";
		JsonNode item = objectMapper.readTree(invalidJson);

		// When
		HeroRanking result = processor.process(item);

		// Then
		assertNull(result);
		verify(heroRankingRepository, never()).findByHeroIdAndAccountId(any(), any());
	}

	@Test
	void testProcess_InvalidAccountIdType_ShouldReturnNull() throws Exception {
		// Given
		String invalidJson = """
				{
					"account_id": "invalid_id",
					"hero_id": 1,
					"score": 95.5
				}
				""";
		JsonNode item = objectMapper.readTree(invalidJson);

		// When
		HeroRanking result = processor.process(item);

		// Then
		assertNull(result);
		verify(heroRankingRepository, never()).findByHeroIdAndAccountId(any(), any());
	}

	@Test
	void testProcess_InvalidHeroIdType_ShouldReturnNull() throws Exception {
		// Given
		String invalidJson = """
				{
					"account_id": 12345,
					"hero_id": "invalid_hero_id",
					"score": 95.5
				}
				""";
		JsonNode item = objectMapper.readTree(invalidJson);

		// When
		HeroRanking result = processor.process(item);

		// Then
		assertNull(result);
		verify(heroRankingRepository, never()).findByHeroIdAndAccountId(any(), any());
	}

	@Test
	void testProcess_NegativeAccountId_ShouldReturnNull() throws Exception {
		// Given
		String invalidJson = """
				{
					"account_id": -12345,
					"hero_id": 1,
					"score": 95.5
				}
				""";
		JsonNode item = objectMapper.readTree(invalidJson);

		// When
		HeroRanking result = processor.process(item);

		// Then
		assertNull(result);
		verify(heroRankingRepository, never()).findByHeroIdAndAccountId(any(), any());
	}

	@Test
	void testProcess_NegativeHeroId_ShouldReturnNull() throws Exception {
		// Given
		String invalidJson = """
				{
					"account_id": 12345,
					"hero_id": -1,
					"score": 95.5
				}
				""";
		JsonNode item = objectMapper.readTree(invalidJson);

		// When
		HeroRanking result = processor.process(item);

		// Then
		assertNull(result);
		verify(heroRankingRepository, never()).findByHeroIdAndAccountId(any(), any());
	}

	@Test
	void testProcess_ZeroAccountId_ShouldReturnNull() throws Exception {
		// Given
		String invalidJson = """
				{
					"account_id": 0,
					"hero_id": 1,
					"score": 95.5
				}
				""";
		JsonNode item = objectMapper.readTree(invalidJson);

		// When
		HeroRanking result = processor.process(item);

		// Then
		assertNull(result);
		verify(heroRankingRepository, never()).findByHeroIdAndAccountId(any(), any());
	}

	@Test
	void testProcess_ZeroHeroId_ShouldReturnNull() throws Exception {
		// Given
		String invalidJson = """
				{
					"account_id": 12345,
					"hero_id": 0,
					"score": 95.5
				}
				""";
		JsonNode item = objectMapper.readTree(invalidJson);

		// When
		HeroRanking result = processor.process(item);

		// Then
		assertNull(result);
		verify(heroRankingRepository, never()).findByHeroIdAndAccountId(any(), any());
	}

	@Test
	void testProcess_WithDifferentScoreTypes_ShouldHandleCorrectly() throws Exception {
		// Given
		String validJson = """
				{
					"account_id": 12345,
					"hero_id": 1,
					"score": 100
				}
				""";
		JsonNode item = objectMapper.readTree(validJson);

		when(heroRankingRepository.findByHeroIdAndAccountId(12345L, 1)).thenReturn(Optional.empty());

		// When
		HeroRanking result = processor.process(item);

		// Then
		assertNotNull(result);
		assertEquals(100.0, result.getScore());
	}

	@Test
	void testProcess_RepositoryException_ShouldThrowCustomException() {
		// Given
		String validJson = """
				{
					"account_id": 12345,
					"hero_id": 1,
					"score": 95.5
				}
				""";
		JsonNode item;
		try {
			item = objectMapper.readTree(validJson);
		}
		catch (Exception e) {
			fail("Failed to parse test JSON");
			return;
		}

		when(heroRankingRepository.findByHeroIdAndAccountId(12345L, 1))
			.thenThrow(new RuntimeException("Database error"));

		// When & Then
		HeroRankingProcessor.HeroRankingProcessingException exception = assertThrows(
				HeroRankingProcessor.HeroRankingProcessingException.class, () -> processor.process(item));

		assertEquals("Failed to process hero ranking data", exception.getMessage());
		assertNotNull(exception.getCause());
		assertEquals("Database error", exception.getCause().getMessage());
	}

	@Test
	void testProcess_WithMaxValues_ShouldHandleCorrectly() throws Exception {
		// Given
		String validJson = """
				{
					"account_id": 9223372036854775807,
					"hero_id": 2147483647,
					"score": 100.0
				}
				""";
		JsonNode item = objectMapper.readTree(validJson);

		when(heroRankingRepository.findByHeroIdAndAccountId(9223372036854775807L, 2147483647))
			.thenReturn(Optional.empty());

		// When
		HeroRanking result = processor.process(item);

		// Then
		assertNotNull(result);
		assertEquals(9223372036854775807L, result.getAccountId());
		assertEquals(2147483647, result.getHeroId());
		assertEquals(100.0, result.getScore());
	}

}
