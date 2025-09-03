package com.abe.gg_stats.batch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.abe.gg_stats.batch.hero.HeroProcessor;
import com.abe.gg_stats.config.JacksonConfig;
import com.abe.gg_stats.dto.HeroDto;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

class HeroProcessorTest {

	private HeroProcessor heroProcessor;

	private ObjectMapper objectMapper;

	@BeforeEach
	void setUp() {
		objectMapper = new JacksonConfig().objectMapper();
		heroProcessor = new HeroProcessor(objectMapper);
	}

	@Test
	void testProcess_ValidData_ShouldSucceed() throws JsonProcessingException {
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

		HeroDto result = heroProcessor.process(heroData);

		assertNotNull(result);
		assertEquals(1, result.id());
		assertEquals("antimage", result.name());
		assertEquals("Anti-Mage", result.localizedName());
		assertEquals("agi", result.primaryAttr());
		assertEquals("Melee", result.attackType());
		assertNotNull(result.roles());
		assertEquals(3, result.roles().size());
		assertTrue(result.roles().contains("Carry"));
		assertTrue(result.roles().contains("Escape"));
		assertTrue(result.roles().contains("Nuker"));
	}

	@Test
	void testProcess_ValidDataWithoutOptionalFields_ShouldSucceed() throws JsonProcessingException {
		String validHeroJson = """
				{
				    "id": 2,
				    "name": "axe",
				    "localized_name": "Axe"
				}
				""";
		JsonNode heroData = objectMapper.readTree(validHeroJson);

		HeroDto result = heroProcessor.process(heroData);

		assertNotNull(result);
		assertEquals(2, result.id());
		assertEquals("axe", result.name());
		assertEquals("Axe", result.localizedName());
		assertNull(result.primaryAttr());
		assertNull(result.attackType());
		assertNotNull(result.roles());
		assertTrue(result.roles().isEmpty());
	}

	@Test
	void testProcess_ValidDataWithEmptyRoles_ShouldSucceed() throws JsonProcessingException {
		String validHeroJson = """
				{
				    "id": 3,
				    "name": "crystal_maiden",
				    "localized_name": "Crystal Maiden",
				    "roles": []
				}
				""";
		JsonNode heroData = objectMapper.readTree(validHeroJson);

		HeroDto result = heroProcessor.process(heroData);

		assertNotNull(result);
		assertEquals(3, result.id());
		assertEquals("crystal_maiden", result.name());
		assertEquals("Crystal Maiden", result.localizedName());
		assertNotNull(result.roles());
		assertTrue(result.roles().isEmpty());
	}

	@Test
	void testProcess_MissingId_ShouldReturnNull() throws JsonProcessingException {
		String invalidHeroJson = """
				{
				    "name": "antimage",
				    "localized_name": "Anti-Mage"
				}
				""";
		JsonNode heroData = objectMapper.readTree(invalidHeroJson);

		HeroDto result = heroProcessor.process(heroData);

		assertNull(result);
	}

	@Test
	void testProcess_MissingName_ShouldReturnNull() throws JsonProcessingException {
		String invalidHeroJson = """
				{
				    "id": 1,
				    "localized_name": "Anti-Mage"
				}
				""";
		JsonNode heroData = objectMapper.readTree(invalidHeroJson);

		HeroDto result = heroProcessor.process(heroData);

		assertNull(result);
	}

	@Test
	void testProcess_MissingLocalizedName_ShouldReturnNull() throws JsonProcessingException {
		String invalidHeroJson = """
				{
				    "id": 1,
				    "name": "antimage"
				}
				""";
		JsonNode heroData = objectMapper.readTree(invalidHeroJson);

		HeroDto result = heroProcessor.process(heroData);

		assertNull(result);
	}

	@Test
	void testProcess_EmptyName_ShouldReturnNull() throws JsonProcessingException {
		String invalidHeroJson = """
				{
				    "id": 1,
				    "name": "",
				    "localized_name": "Anti-Mage"
				}
				""";
		JsonNode heroData = objectMapper.readTree(invalidHeroJson);

		HeroDto result = heroProcessor.process(heroData);

		assertNull(result);
	}

	@Test
	void testProcess_InvalidId_ShouldReturnNull() throws JsonProcessingException {
		String invalidHeroJson = """
				{
				    "id": -1,
				    "name": "antimage",
				    "localized_name": "Anti-Mage"
				}
				""";
		JsonNode heroData = objectMapper.readTree(invalidHeroJson);

		HeroDto result = heroProcessor.process(heroData);

		assertNull(result);
	}

	@Test
	void testProcess_ZeroId_ShouldReturnNull() throws JsonProcessingException {
		String invalidHeroJson = """
				{
				    "id": 0,
				    "name": "antimage",
				    "localized_name": "Anti-Mage"
				}
				""";
		JsonNode heroData = objectMapper.readTree(invalidHeroJson);

		HeroDto result = heroProcessor.process(heroData);

		assertNull(result);
	}

	@Test
	void testProcess_NonNumericId_ShouldReturnNull() throws JsonProcessingException {
		String invalidHeroJson = """
				{
				    "id": "invalid",
				    "name": "antimage",
				    "localized_name": "Anti-Mage"
				}
				""";
		JsonNode heroData = objectMapper.readTree(invalidHeroJson);

		HeroDto result = heroProcessor.process(heroData);

		assertNull(result);
	}

	@Test
	void testProcess_RolesWithNullValues_ShouldFilterOutNulls() throws JsonProcessingException {
		String validHeroJson = """
				{
				    "id": 4,
				    "name": "test_hero",
				    "localized_name": "Test Hero",
				    "roles": ["Carry", null, "Support", "", "Nuker"]
				}
				""";
		JsonNode heroData = objectMapper.readTree(validHeroJson);

		HeroDto result = heroProcessor.process(heroData);

		assertNotNull(result);
		assertEquals(4, result.id());
		assertNotNull(result.roles());
		assertEquals(3, result.roles().size());
		assertTrue(result.roles().contains("Carry"));
		assertTrue(result.roles().contains("Support"));
		assertTrue(result.roles().contains("Nuker"));
	}

}
