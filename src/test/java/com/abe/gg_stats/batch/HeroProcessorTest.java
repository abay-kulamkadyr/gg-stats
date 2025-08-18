package com.abe.gg_stats.batch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.abe.gg_stats.batch.hero.HeroProcessor;
import com.abe.gg_stats.entity.Hero;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class HeroProcessorTest {

	private HeroProcessor heroProcessor;

	private ObjectMapper objectMapper;

	@BeforeEach
	void setUp() {
		heroProcessor = new HeroProcessor();
		objectMapper = new ObjectMapper();
	}

	@Test
	void testProcess_ValidData_ShouldSucceed() throws JsonProcessingException {
		// Given
		String validHeroJson = """
				{
				    "id": 1,
				    "name": "antimage",
				    "localized_name": "Anti-Mage",
				    "primary_attr": "agi",
				    "attack_type": "Melee",
				    "roles": ["Carry", "Escape", "Nuker"]
				}
				""";
		JsonNode heroData = objectMapper.readTree(validHeroJson);

		// When
		Hero result = heroProcessor.process(heroData);

		// Then
		assertNotNull(result);
		assertEquals(1, result.getId());
		assertEquals("antimage", result.getName());
		assertEquals("Anti-Mage", result.getLocalizedName());
		assertEquals("agi", result.getPrimaryAttr());
		assertEquals("Melee", result.getAttackType());
		assertNotNull(result.getRoles());
		assertEquals(3, result.getRoles().size());
		assertTrue(result.getRoles().contains("Carry"));
		assertTrue(result.getRoles().contains("Escape"));
		assertTrue(result.getRoles().contains("Nuker"));
	}

	@Test
	void testProcess_ValidDataWithoutOptionalFields_ShouldSucceed() throws JsonProcessingException {
		// Given
		String validHeroJson = """
				{
				    "id": 2,
				    "name": "axe",
				    "localized_name": "Axe"
				}
				""";
		JsonNode heroData = objectMapper.readTree(validHeroJson);

		// When
		Hero result = heroProcessor.process(heroData);

		// Then
		assertNotNull(result);
		assertEquals(2, result.getId());
		assertEquals("axe", result.getName());
		assertEquals("Axe", result.getLocalizedName());
		assertNull(result.getPrimaryAttr());
		assertNull(result.getAttackType());
		assertNotNull(result.getRoles());
		assertTrue(result.getRoles().isEmpty());
	}

	@Test
	void testProcess_ValidDataWithEmptyRoles_ShouldSucceed() throws JsonProcessingException {
		// Given
		String validHeroJson = """
				{
				    "id": 3,
				    "name": "crystal_maiden",
				    "localized_name": "Crystal Maiden",
				    "roles": []
				}
				""";
		JsonNode heroData = objectMapper.readTree(validHeroJson);

		// When
		Hero result = heroProcessor.process(heroData);

		// Then
		assertNotNull(result);
		assertEquals(3, result.getId());
		assertEquals("crystal_maiden", result.getName());
		assertEquals("Crystal Maiden", result.getLocalizedName());
		assertNotNull(result.getRoles());
		assertTrue(result.getRoles().isEmpty());
	}

	@Test
	void testProcess_MissingId_ShouldReturnNull() throws JsonProcessingException {
		// Given
		String invalidHeroJson = """
				{
				    "name": "antimage",
				    "localized_name": "Anti-Mage"
				}
				""";
		JsonNode heroData = objectMapper.readTree(invalidHeroJson);

		// When
		Hero result = heroProcessor.process(heroData);

		// Then
		assertNull(result);
	}

	@Test
	void testProcess_MissingName_ShouldReturnNull() throws JsonProcessingException {
		// Given
		String invalidHeroJson = """
				{
				    "id": 1,
				    "localized_name": "Anti-Mage"
				}
				""";
		JsonNode heroData = objectMapper.readTree(invalidHeroJson);

		// When
		Hero result = heroProcessor.process(heroData);

		// Then
		assertNull(result);
	}

	@Test
	void testProcess_MissingLocalizedName_ShouldReturnNull() throws JsonProcessingException {
		// Given
		String invalidHeroJson = """
				{
				    "id": 1,
				    "name": "antimage"
				}
				""";
		JsonNode heroData = objectMapper.readTree(invalidHeroJson);

		// When
		Hero result = heroProcessor.process(heroData);

		// Then
		assertNull(result);
	}

	@Test
	void testProcess_EmptyName_ShouldReturnNull() throws JsonProcessingException {
		// Given
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
	}

	@Test
	void testProcess_InvalidId_ShouldReturnNull() throws JsonProcessingException {
		// Given
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
	}

	@Test
	void testProcess_ZeroId_ShouldReturnNull() throws JsonProcessingException {
		// Given
		String invalidHeroJson = """
				{
				    "id": 0,
				    "name": "antimage",
				    "localized_name": "Anti-Mage"
				}
				""";
		JsonNode heroData = objectMapper.readTree(invalidHeroJson);

		// When
		Hero result = heroProcessor.process(heroData);

		// Then
		assertNull(result);
	}

	@Test
	void testProcess_NonNumericId_ShouldReturnNull() throws JsonProcessingException {
		// Given
		String invalidHeroJson = """
				{
				    "id": "invalid",
				    "name": "antimage",
				    "localized_name": "Anti-Mage"
				}
				""";
		JsonNode heroData = objectMapper.readTree(invalidHeroJson);

		// When
		Hero result = heroProcessor.process(heroData);

		// Then
		assertNull(result);
	}

	@Test
	void testProcess_RolesWithNullValues_ShouldFilterOutNulls() throws JsonProcessingException {
		// Given
		String validHeroJson = """
				{
				    "id": 4,
				    "name": "test_hero",
				    "localized_name": "Test Hero",
				    "roles": ["Carry", null, "Support", "", "Nuker"]
				}
				""";
		JsonNode heroData = objectMapper.readTree(validHeroJson);

		// When
		Hero result = heroProcessor.process(heroData);

		// Then
		assertNotNull(result);
		assertEquals(4, result.getId());
		assertNotNull(result.getRoles());
		assertEquals(3, result.getRoles().size());
		assertTrue(result.getRoles().contains("Carry"));
		assertTrue(result.getRoles().contains("Support"));
		assertTrue(result.getRoles().contains("Nuker")); // Valid role should be included
		// null and empty string should be filtered out
	}

}
