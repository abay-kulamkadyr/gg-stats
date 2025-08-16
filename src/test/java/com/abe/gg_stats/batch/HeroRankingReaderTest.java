package com.abe.gg_stats.batch;

import com.abe.gg_stats.batch.heroRanking.HeroRankingReader;
import com.abe.gg_stats.config.BatchExpirationConfig;
import com.abe.gg_stats.repository.HeroRepository;
import com.abe.gg_stats.service.OpenDotaApiService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HeroRankingReaderTest {

	@Mock
	private HeroRepository heroRepository;

	@Mock
	private OpenDotaApiService openDotaApiService;

	private HeroRankingReader reader;

	private ObjectMapper objectMapper;

	@Mock
	private BatchExpirationConfig batchExpirationConfig;

	@BeforeEach
	void setUp() {
		reader = new HeroRankingReader(openDotaApiService, heroRepository, batchExpirationConfig);
		objectMapper = new ObjectMapper();
	}

	@Test
	void testRead_FirstCall_ShouldInitializeAndFetchFirstHero() throws Exception {
		// Given
		when(heroRepository.findAllIds()).thenReturn(Arrays.asList(1, 2, 3));

		ObjectNode responseNode = objectMapper.createObjectNode();
		ArrayNode rankingsArray = objectMapper.createArrayNode();

		ObjectNode ranking1 = objectMapper.createObjectNode();
		ranking1.put("account_id", 12345L);
		ranking1.put("score", 95.5);

		ObjectNode ranking2 = objectMapper.createObjectNode();
		ranking2.put("account_id", 67890L);
		ranking2.put("score", 88.0);

		rankingsArray.add(ranking1);
		rankingsArray.add(ranking2);
		responseNode.set("rankings", rankingsArray);

		when(openDotaApiService.getHeroRanking(1)).thenReturn(Optional.of(responseNode));

		// When
		JsonNode result = reader.read();

		// Then
		assertNotNull(result);
		assertTrue(result.has("account_id"));
		assertTrue(result.has("score"));
		assertTrue(result.has("hero_id"));
		assertEquals(1, result.get("hero_id").asInt());

		verify(heroRepository, times(2)).findAllIds(); // Called once for iterator, once
														// for logging
		verify(openDotaApiService).getHeroRanking(1);
	}

	@Test
	void testRead_SubsequentCalls_ShouldReturnNextRankings() throws Exception {
		// Given
		when(heroRepository.findAllIds()).thenReturn(Arrays.asList(1));

		ObjectNode responseNode = objectMapper.createObjectNode();
		ArrayNode rankingsArray = objectMapper.createArrayNode();

		ObjectNode ranking1 = objectMapper.createObjectNode();
		ranking1.put("account_id", 12345L);
		ranking1.put("score", 95.5);

		ObjectNode ranking2 = objectMapper.createObjectNode();
		ranking2.put("account_id", 67890L);
		ranking2.put("score", 88.0);

		rankingsArray.add(ranking1);
		rankingsArray.add(ranking2);
		responseNode.set("rankings", rankingsArray);

		when(openDotaApiService.getHeroRanking(1)).thenReturn(Optional.of(responseNode));

		// When - First call initializes and returns first ranking
		JsonNode firstResult = reader.read();
		// Second call should return second ranking
		JsonNode secondResult = reader.read();
		// Third call should return null (end of data)
		JsonNode thirdResult = reader.read();

		// Then
		assertNotNull(firstResult);
		assertNotNull(secondResult);
		assertNull(thirdResult);

		assertEquals(1, firstResult.get("hero_id").asInt());
		assertEquals(1, secondResult.get("hero_id").asInt());
	}

	@Test
	void testRead_WithEmptyRankings_ShouldMoveToNextHero() throws Exception {
		// Given
		when(heroRepository.findAllIds()).thenReturn(Arrays.asList(1, 2));

		// First hero has empty rankings
		ObjectNode emptyResponse = objectMapper.createObjectNode();
		ArrayNode emptyRankings = objectMapper.createArrayNode();
		emptyResponse.set("rankings", emptyRankings);

		// Second hero has rankings
		ObjectNode validResponse = objectMapper.createObjectNode();
		ArrayNode rankingsArray = objectMapper.createArrayNode();

		ObjectNode ranking = objectMapper.createObjectNode();
		ranking.put("account_id", 12345L);
		ranking.put("score", 95.5);

		rankingsArray.add(ranking);
		validResponse.set("rankings", rankingsArray);

		when(openDotaApiService.getHeroRanking(1)).thenReturn(Optional.of(emptyResponse));
		when(openDotaApiService.getHeroRanking(2)).thenReturn(Optional.of(validResponse));

		// When
		JsonNode result = reader.read();

		// Then
		assertNotNull(result);
		assertEquals(2, result.get("hero_id").asInt());

		verify(openDotaApiService).getHeroRanking(1);
		verify(openDotaApiService).getHeroRanking(2);
	}

	@Test
	void testRead_WithNoRankingsField_ShouldMoveToNextHero() throws Exception {
		// Given
		when(heroRepository.findAllIds()).thenReturn(Arrays.asList(1, 2));

		// First hero has no rankings field
		ObjectNode invalidResponse = objectMapper.createObjectNode();
		invalidResponse.put("other_field", "value");

		// Second hero has valid rankings
		ObjectNode validResponse = objectMapper.createObjectNode();
		ArrayNode rankingsArray = objectMapper.createArrayNode();

		ObjectNode ranking = objectMapper.createObjectNode();
		ranking.put("account_id", 12345L);
		ranking.put("score", 95.5);

		rankingsArray.add(ranking);
		validResponse.set("rankings", rankingsArray);

		when(openDotaApiService.getHeroRanking(1)).thenReturn(Optional.of(invalidResponse));
		when(openDotaApiService.getHeroRanking(2)).thenReturn(Optional.of(validResponse));

		// When
		JsonNode result = reader.read();

		// Then
		assertNotNull(result);
		assertEquals(2, result.get("hero_id").asInt());
	}

	@Test
	void testRead_WithNonArrayRankings_ShouldMoveToNextHero() throws Exception {
		// Given
		when(heroRepository.findAllIds()).thenReturn(Arrays.asList(1, 2));

		// First hero has rankings as string instead of array
		ObjectNode invalidResponse = objectMapper.createObjectNode();
		invalidResponse.put("rankings", "not_an_array");

		// Second hero has valid rankings
		ObjectNode validResponse = objectMapper.createObjectNode();
		ArrayNode rankingsArray = objectMapper.createArrayNode();

		ObjectNode ranking = objectMapper.createObjectNode();
		ranking.put("account_id", 12345L);
		ranking.put("score", 95.5);

		rankingsArray.add(ranking);
		validResponse.set("rankings", rankingsArray);

		when(openDotaApiService.getHeroRanking(1)).thenReturn(Optional.of(invalidResponse));
		when(openDotaApiService.getHeroRanking(2)).thenReturn(Optional.of(validResponse));

		// When
		JsonNode result = reader.read();

		// Then
		assertNotNull(result);
		assertEquals(2, result.get("hero_id").asInt());
	}

	@Test
	void testRead_WithNoApiResponse_ShouldMoveToNextHero() throws Exception {
		// Given
		when(heroRepository.findAllIds()).thenReturn(Arrays.asList(1, 2));

		// First hero has no API response
		when(openDotaApiService.getHeroRanking(1)).thenReturn(Optional.empty());

		// Second hero has valid response
		ObjectNode validResponse = objectMapper.createObjectNode();
		ArrayNode rankingsArray = objectMapper.createArrayNode();

		ObjectNode ranking = objectMapper.createObjectNode();
		ranking.put("account_id", 12345L);
		ranking.put("score", 95.5);

		rankingsArray.add(ranking);
		validResponse.set("rankings", rankingsArray);

		when(openDotaApiService.getHeroRanking(2)).thenReturn(Optional.of(validResponse));

		// When
		JsonNode result = reader.read();

		// Then
		assertNotNull(result);
		assertEquals(2, result.get("hero_id").asInt());
	}

	@Test
	void testRead_WithAllHeroesProcessed_ShouldReturnNull() throws Exception {
		// Given
		when(heroRepository.findAllIds()).thenReturn(Arrays.asList(1));

		ObjectNode responseNode = objectMapper.createObjectNode();
		ArrayNode rankingsArray = objectMapper.createArrayNode();

		ObjectNode ranking = objectMapper.createObjectNode();
		ranking.put("account_id", 12345L);
		ranking.put("score", 95.5);

		rankingsArray.add(ranking);
		responseNode.set("rankings", rankingsArray);

		when(openDotaApiService.getHeroRanking(1)).thenReturn(Optional.of(responseNode));

		// When
		JsonNode firstResult = reader.read();
		JsonNode secondResult = reader.read();

		// Then
		assertNotNull(firstResult);
		assertNull(secondResult); // Should return null after all rankings processed
	}

	@Test
	void testRead_WithEmptyHeroList_ShouldReturnNull() throws Exception {
		// Given
		when(heroRepository.findAllIds()).thenReturn(Arrays.asList());

		// When
		JsonNode result = reader.read();

		// Then
		assertNull(result);
		verify(heroRepository, times(2)).findAllIds(); // Called once for iterator, once
														// for logging
		verify(openDotaApiService, never()).getHeroRanking(any());
	}

	@Test
	void testRead_WithMultipleHeroes_ShouldProcessAllHeroes() throws Exception {
		// Given
		when(heroRepository.findAllIds()).thenReturn(Arrays.asList(1, 2, 3));

		// Hero 1 has rankings
		ObjectNode response1 = objectMapper.createObjectNode();
		ArrayNode rankings1 = objectMapper.createArrayNode();
		ObjectNode ranking1 = objectMapper.createObjectNode();
		ranking1.put("account_id", 12345L);
		ranking1.put("score", 95.5);
		rankings1.add(ranking1);
		response1.set("rankings", rankings1);

		// Hero 2 has no rankings
		ObjectNode response2 = objectMapper.createObjectNode();
		ArrayNode rankings2 = objectMapper.createArrayNode();
		response2.set("rankings", rankings2);

		// Hero 3 has rankings
		ObjectNode response3 = objectMapper.createObjectNode();
		ArrayNode rankings3 = objectMapper.createArrayNode();
		ObjectNode ranking3 = objectMapper.createObjectNode();
		ranking3.put("account_id", 67890L);
		ranking3.put("score", 88.0);
		rankings3.add(ranking3);
		response3.set("rankings", rankings3);

		when(openDotaApiService.getHeroRanking(1)).thenReturn(Optional.of(response1));
		when(openDotaApiService.getHeroRanking(2)).thenReturn(Optional.of(response2));
		when(openDotaApiService.getHeroRanking(3)).thenReturn(Optional.of(response3));

		// When
		JsonNode firstResult = reader.read();
		JsonNode secondResult = reader.read();
		JsonNode thirdResult = reader.read();

		// Then
		assertNotNull(firstResult);
		assertNotNull(secondResult);
		assertNull(thirdResult); // Should return null after all rankings processed

		assertEquals(1, firstResult.get("hero_id").asInt());
		assertEquals(3, secondResult.get("hero_id").asInt()); // Should skip hero 2
	}

	@Test
	void testRead_WithException_ShouldThrowCustomException() {
		// Given
		when(heroRepository.findAllIds()).thenThrow(new RuntimeException("Database error"));

		// When & Then
		HeroRankingReader.HeroRankingReadException exception = assertThrows(
				HeroRankingReader.HeroRankingReadException.class, () -> reader.read());

		assertEquals("Failed to read hero ranking data", exception.getMessage());
		assertNotNull(exception.getCause());
		assertEquals("Database error", exception.getCause().getMessage());
	}

	@Test
	void testRead_WithLargeHeroList_ShouldHandleCorrectly() throws Exception {
		// Given
		java.util.List<Integer> heroIds = new ArrayList<>();
		for (int i = 0; i < 100; i++) {
			heroIds.add(i);
		}
		when(heroRepository.findAllIds()).thenReturn(heroIds);

		// All heroes have empty rankings
		ObjectNode emptyResponse = objectMapper.createObjectNode();
		ArrayNode emptyRankings = objectMapper.createArrayNode();
		emptyResponse.set("rankings", emptyRankings);

		for (int i = 0; i < 100; i++) {
			when(openDotaApiService.getHeroRanking(i)).thenReturn(Optional.of(emptyResponse));
		}

		// When
		JsonNode result = reader.read();

		// Then
		assertNull(result); // Should return null after processing all heroes
		verify(openDotaApiService, times(100)).getHeroRanking(any());
	}

}
