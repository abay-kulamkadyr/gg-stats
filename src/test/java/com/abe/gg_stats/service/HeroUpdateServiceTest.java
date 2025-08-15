package com.abe.gg_stats.service;

import com.abe.gg_stats.entity.Hero;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class HeroUpdateServiceTest {

	private HeroUpdateService heroUpdateService;

	private ObjectMapper objectMapper;

	@BeforeEach
	void setUp() {
		heroUpdateService = new HeroUpdateService();
		objectMapper = new ObjectMapper();
	}

	@Test
	void testProcessHeroData_ValidData_ShouldSucceed() throws Exception {
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
		Hero hero = heroUpdateService.processHeroData(heroData);

		// Then
		assertNotNull(hero);
		assertEquals(1, hero.getId());
		assertEquals("antimage", hero.getName());
		assertEquals("Anti-Mage", hero.getLocalizedName());
		assertEquals("agi", hero.getPrimaryAttr());
		assertEquals("Melee", hero.getAttackType());
		assertEquals(3, hero.getRoles().size());
		assertTrue(hero.getRoles().contains("Carry"));
		assertTrue(hero.getRoles().contains("Escape"));
		assertTrue(hero.getRoles().contains("Nuker"));
	}

	@Test
	void testProcessHeroData_MinimalData_ShouldSucceed() throws Exception {
		// Given
		String minimalHeroJson = """
				{
					"id": 2,
					"name": "axe",
					"localized_name": "Axe"
				}
				""";
		JsonNode heroData = objectMapper.readTree(minimalHeroJson);

		// When
		Hero hero = heroUpdateService.processHeroData(heroData);

		// Then
		assertNotNull(hero);
		assertEquals(2, hero.getId());
		assertEquals("axe", hero.getName());
		assertEquals("Axe", hero.getLocalizedName());
		assertNull(hero.getPrimaryAttr());
		assertNull(hero.getAttackType());
		assertTrue(hero.getRoles().isEmpty());
	}

	@Test
	void testProcessHeroData_EmptyRolesArray_ShouldHandleGracefully() throws Exception {
		// Given
		String heroWithEmptyRolesJson = """
				{
					"id": 3,
					"name": "crystal_maiden",
					"localized_name": "Crystal Maiden",
					"roles": []
				}
				""";
		JsonNode heroData = objectMapper.readTree(heroWithEmptyRolesJson);

		// When
		Hero hero = heroUpdateService.processHeroData(heroData);

		// Then
		assertNotNull(hero);
		assertEquals(3, hero.getId());
		assertTrue(hero.getRoles().isEmpty());
	}

	@Test
	void testProcessHeroData_NullData_ShouldThrowException() {
		// When & Then
		HeroUpdateService.HeroProcessingException exception = assertThrows(
				HeroUpdateService.HeroProcessingException.class, () -> heroUpdateService.processHeroData(null));
		assertEquals("Hero data is null", exception.getMessage());
	}

	@Test
	void testProcessHeroData_MissingId_ShouldThrowException() throws Exception {
		// Given
		String heroWithoutIdJson = """
				{
					"name": "invalid_hero",
					"localized_name": "Invalid Hero"
				}
				""";
		JsonNode heroData = objectMapper.readTree(heroWithoutIdJson);

		// When & Then
		HeroUpdateService.HeroProcessingException exception = assertThrows(
				HeroUpdateService.HeroProcessingException.class, () -> heroUpdateService.processHeroData(heroData));
		assertEquals("Hero ID is required", exception.getMessage());
	}

	@Test
	void testProcessHeroData_MissingName_ShouldThrowException() throws Exception {
		// Given
		String heroWithoutNameJson = """
				{
					"id": 4,
					"localized_name": "Hero Without Name"
				}
				""";
		JsonNode heroData = objectMapper.readTree(heroWithoutNameJson);

		// When & Then
		HeroUpdateService.HeroProcessingException exception = assertThrows(
				HeroUpdateService.HeroProcessingException.class, () -> heroUpdateService.processHeroData(heroData));
		assertEquals("Hero name is required", exception.getMessage());
	}

	@Test
	void testProcessHeroData_MissingLocalizedName_ShouldThrowException() throws Exception {
		// Given
		String heroWithoutLocalizedNameJson = """
				{
					"id": 5,
					"name": "hero_without_localized_name"
				}
				""";
		JsonNode heroData = objectMapper.readTree(heroWithoutLocalizedNameJson);

		// When & Then
		HeroUpdateService.HeroProcessingException exception = assertThrows(
				HeroUpdateService.HeroProcessingException.class, () -> heroUpdateService.processHeroData(heroData));
		assertEquals("Hero localized name is required", exception.getMessage());
	}

	@Test
	void testProcessHeroData_NullId_ShouldThrowException() throws Exception {
		// Given
		String heroWithNullIdJson = """
				{
					"id": null,
					"name": "hero_with_null_id",
					"localized_name": "Hero With Null ID"
				}
				""";
		JsonNode heroData = objectMapper.readTree(heroWithNullIdJson);

		// When & Then
		HeroUpdateService.HeroProcessingException exception = assertThrows(
				HeroUpdateService.HeroProcessingException.class, () -> heroUpdateService.processHeroData(heroData));
		assertEquals("Hero ID is required", exception.getMessage());
	}

	@Test
	void testProcessHeroData_ComplexRoles_ShouldHandleGracefully() throws Exception {
		// Given
		String heroWithComplexRolesJson = """
				{
					"id": 6,
					"name": "complex_hero",
					"localized_name": "Complex Hero",
					"roles": ["Carry", null, "Support", "", "Nuker"]
				}
				""";
		JsonNode heroData = objectMapper.readTree(heroWithComplexRolesJson);

		// When
		Hero hero = heroUpdateService.processHeroData(heroData);

		// Then
		assertNotNull(hero);
		assertEquals(6, hero.getId());
		List<String> roles = hero.getRoles();
		assertEquals(3, roles.size());
		assertTrue(roles.contains("Carry"));
		assertTrue(roles.contains("Support"));
		assertTrue(roles.contains("Nuker"));
		// Null and empty string roles should be filtered out
		assertFalse(roles.contains(null));
		assertFalse(roles.contains(""));
	}

	@Test
	void testProcessHeroData_AllOptionalFields_ShouldHandleGracefully() throws Exception {
		// Given
		String heroWithAllOptionalFieldsJson = """
				{
					"id": 7,
					"name": "optional_hero",
					"localized_name": "Optional Hero",
					"primary_attr": null,
					"attack_type": "",
					"roles": null
				}
				""";
		JsonNode heroData = objectMapper.readTree(heroWithAllOptionalFieldsJson);

		// When
		Hero hero = heroUpdateService.processHeroData(heroData);

		// Then
		assertNotNull(hero);
		assertEquals(7, hero.getId());
		assertEquals("optional_hero", hero.getName());
		assertEquals("Optional Hero", hero.getLocalizedName());
		assertNull(hero.getPrimaryAttr());
		assertNull(hero.getAttackType());
		// Even when roles is null in JSON, we return an empty list for consistency
		assertTrue(hero.getRoles().isEmpty());
	}

}
