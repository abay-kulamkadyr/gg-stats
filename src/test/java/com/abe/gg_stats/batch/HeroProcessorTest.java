package com.abe.gg_stats.batch;

import com.abe.gg_stats.entity.Hero;
import com.abe.gg_stats.service.HeroUpdateService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HeroProcessorTest {

	@Mock
	private HeroUpdateService heroUpdateService;

	private HeroProcessor heroProcessor;

	private ObjectMapper objectMapper;

	@BeforeEach
	void setUp() {
		heroProcessor = new HeroProcessor(heroUpdateService);
		objectMapper = new ObjectMapper();
	}

	@Test
	void testProcess_ValidData_ShouldSucceed() throws Exception {
		// Given
		String validHeroJson = """
				{
					"id": 1,
					"name": "antimage",
					"localized_name": "Anti-Mage"
				}
				""";
		JsonNode heroData = objectMapper.readTree(validHeroJson);

		Hero expectedHero = new Hero();
		expectedHero.setId(1);
		expectedHero.setName("antimage");
		expectedHero.setLocalizedName("Anti-Mage");

		when(heroUpdateService.processHeroData(heroData)).thenReturn(expectedHero);

		// When
		Hero result = heroProcessor.process(heroData);

		// Then
		assertNotNull(result);
		assertEquals(expectedHero, result);
		verify(heroUpdateService).processHeroData(heroData);
	}

	@Test
	void testProcess_HeroServiceException_ShouldRethrow() throws Exception {
		// Given
		String validHeroJson = """
				{
					"id": 1,
					"name": "antimage",
					"localized_name": "Anti-Mage"
				}
				""";
		JsonNode heroData = objectMapper.readTree(validHeroJson);

		HeroUpdateService.HeroProcessingException serviceException = new HeroUpdateService.HeroProcessingException(
				"Invalid hero data");
		when(heroUpdateService.processHeroData(heroData)).thenThrow(serviceException);

		// When & Then
		HeroUpdateService.HeroProcessingException exception = assertThrows(
				HeroUpdateService.HeroProcessingException.class, () -> heroProcessor.process(heroData));
		assertEquals("Invalid hero data", exception.getMessage());
		verify(heroUpdateService).processHeroData(heroData);
	}

	@Test
	void testProcess_UnexpectedException_ShouldRethrow() throws Exception {
		// Given
		String validHeroJson = """
				{
					"id": 1,
					"name": "antimage",
					"localized_name": "Anti-Mage"
				}
				""";
		JsonNode heroData = objectMapper.readTree(validHeroJson);

		RuntimeException unexpectedException = new RuntimeException("Unexpected error");
		when(heroUpdateService.processHeroData(heroData)).thenThrow(unexpectedException);

		// When & Then
		HeroProcessor.HeroProcessingException exception = assertThrows(HeroProcessor.HeroProcessingException.class,
				() -> heroProcessor.process(heroData));
		assertEquals("Failed to process hero data", exception.getMessage());
		assertEquals(unexpectedException, exception.getCause());
		verify(heroUpdateService).processHeroData(heroData);
	}

	@Test
	void testProcess_NullData_ShouldHandleGracefully() throws Exception {
		// When
		Hero result = heroProcessor.process(null);

		// Then
		assertNull(result);
		verify(heroUpdateService, never()).processHeroData(any());
	}

	@Test
	void testProcess_InvalidData_ShouldHandleGracefully() throws Exception {
		// Given - Invalid hero data missing required fields
		String invalidHeroJson = """
				{
					"name": "antimage"
				}
				""";
		JsonNode heroData = objectMapper.readTree(invalidHeroJson);

		// When
		Hero result = heroProcessor.process(heroData);

		// Then
		assertNull(result);
		verify(heroUpdateService, never()).processHeroData(any());
	}

	@Test
	void testProcess_EmptyName_ShouldHandleGracefully() throws Exception {
		// Given - Hero data with empty name
		String invalidHeroJson = """
				{
					"id": 1,
					"name": "",
					"localized_name": "Anti-Mage"
				}
				""";
		JsonNode heroData = objectMapper.readTree(invalidHeroJson);

		// When
		Hero result = heroProcessor.process(heroData);

		// Then
		assertNull(result);
		verify(heroUpdateService, never()).processHeroData(any());
	}

	@Test
	void testProcess_InvalidId_ShouldHandleGracefully() throws Exception {
		// Given - Hero data with invalid ID
		String invalidHeroJson = """
				{
					"id": -1,
					"name": "antimage",
					"localized_name": "Anti-Mage"
				}
				""";
		JsonNode heroData = objectMapper.readTree(invalidHeroJson);

		// When
		Hero result = heroProcessor.process(heroData);

		// Then
		assertNull(result);
		verify(heroUpdateService, never()).processHeroData(any());
	}

}
