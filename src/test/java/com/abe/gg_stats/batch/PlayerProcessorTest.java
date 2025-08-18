package com.abe.gg_stats.batch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.abe.gg_stats.batch.player.PlayerProcessor;
import com.abe.gg_stats.entity.Player;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PlayerProcessorTest {

	private PlayerProcessor playerProcessor;

	private ObjectMapper objectMapper;

	@BeforeEach
	void setUp() {
		playerProcessor = new PlayerProcessor();
		objectMapper = new ObjectMapper();
	}

	@Test
	void testProcess_ValidPlayerData_ShouldSucceed() throws JsonProcessingException {
		// Given
		String validJson = """
				{
					"account_id": 12345,
					"profile": {
						"steamid": "76561198012345678",
						"personaname": "TestPlayer",
						"avatar": "http://example.com/avatar.jpg",
						"avatarmedium": "http://example.com/avatar_medium.jpg",
						"avatarfull": "http://example.com/avatar_full.jpg",
						"profileurl": "http://steamcommunity.com/id/testplayer",
						"last_login": "1640995200",
						"full_history_time": "1640995200",
						"last_match_time": "1640995200",
						"cheese": 100,
						"fh_unavailable": false,
						"loccountrycode": "US",
						"plus": true
					},
					"rank_tier": 80,
					"leaderboard_rank": 150
				}
				""";
		JsonNode playerData = objectMapper.readTree(validJson);

		// When
		Player result = playerProcessor.process(playerData);

		// Then
		assertNotNull(result);
		assertEquals("76561198012345678", result.getSteamId());
		assertEquals("TestPlayer", result.getPersonName());
		assertEquals("http://example.com/avatar.jpg", result.getAvatar());
		assertEquals("http://example.com/avatar_medium.jpg", result.getAvatarMedium());
		assertEquals("http://example.com/avatar_full.jpg", result.getAvatarFull());
		assertEquals("http://steamcommunity.com/id/testplayer", result.getProfileUrl());
		assertEquals(100, result.getCheese());
		assertFalse(result.getFhUnavailable());
		assertEquals("US", result.getLocCountryCode());
		assertTrue(result.getPlus());
		assertEquals(80, result.getRankTier());
		assertEquals(150, result.getLeaderboardRank());
	}

	@Test
	void testProcess_ValidDataWithoutOptionalFields_ShouldSucceed() throws JsonProcessingException {
		// Given
		String validJson = """
				{
					"profile": {
						"steamid": "76561198012345678",
						"personaname": "TestPlayer"
					}
				}
				""";
		JsonNode playerData = objectMapper.readTree(validJson);

		// When
		Player result = playerProcessor.process(playerData);

		// Then
		assertNotNull(result);
		assertEquals("76561198012345678", result.getSteamId());
		assertEquals("TestPlayer", result.getPersonName());
		assertNull(result.getAvatar());
		assertNull(result.getAvatarMedium());
		assertNull(result.getAvatarFull());
		assertNull(result.getProfileUrl());
		assertNull(result.getCheese());
		assertNull(result.getFhUnavailable());
		assertNull(result.getLocCountryCode());
		assertNull(result.getPlus());
		assertNull(result.getRankTier());
		assertNull(result.getLeaderboardRank());
	}

	@Test
	void testProcess_ValidDataWithAccountId_ShouldSucceed() throws JsonProcessingException {
		// Given
		String validJson = """
				{
					"account_id": 12345,
					"profile": {
						"steamid": "76561198012345678",
						"personaname": "TestPlayer"
					}
				}
				""";
		JsonNode playerData = objectMapper.readTree(validJson);

		// When
		Player result = playerProcessor.process(playerData);

		// Then
		assertNotNull(result);
		assertEquals("76561198012345678", result.getSteamId());
		assertEquals("TestPlayer", result.getPersonName());
	}

	@Test
	void testProcess_NullItem_ShouldThrowException() {
		// When & Then - BaseProcessor.process has @NonNull annotation, so it should throw
		// NullPointerException
		assertThrows(NullPointerException.class, () -> playerProcessor.process(null));
	}

	@Test
	void testProcess_MissingProfile_ShouldCreatePlayerWithoutProfileData() throws Exception {
		// Given
		String invalidJson = """
				{
					"account_id": 12345
				}
				""";
		JsonNode playerData = objectMapper.readTree(invalidJson);

		// When
		Player result = playerProcessor.process(playerData);

		// Then - Processor creates a Player object even when profile data is missing
		assertNotNull(result);
		assertNull(result.getSteamId());
		assertNull(result.getPersonName());
		// Other profile fields should also be null
		assertNull(result.getAvatar());
		assertNull(result.getAvatarMedium());
		assertNull(result.getAvatarFull());
		assertNull(result.getProfileUrl());
	}

	@Test
	void testProcess_MissingSteamId_ShouldReturnNull() throws JsonProcessingException {
		// Given
		String invalidJson = """
				{
					"profile": {
						"personaname": "TestPlayer"
					}
				}
				""";
		JsonNode playerData = objectMapper.readTree(invalidJson);

		// When
		Player result = playerProcessor.process(playerData);

		// Then - Processor rejects invalid input (missing steamid) and returns null
		assertNull(result);
	}

	@Test
	void testProcess_MissingPersonName_ShouldReturnNull() throws JsonProcessingException {
		// Given
		String invalidJson = """
				{
					"profile": {
						"steamid": "76561198012345678"
					}
				}
				""";
		JsonNode playerData = objectMapper.readTree(invalidJson);

		// When
		Player result = playerProcessor.process(playerData);

		// Then - Processor rejects invalid input (missing personaname) and returns null
		assertNull(result);
	}

	@Test
	void testProcess_EmptySteamId_ShouldReturnNull() throws JsonProcessingException {
		// Given
		String invalidJson = """
				{
					"profile": {
						"steamid": "",
						"personaname": "TestPlayer"
					}
				}
				""";
		JsonNode playerData = objectMapper.readTree(invalidJson);

		// When
		Player result = playerProcessor.process(playerData);

		// Then - Processor rejects invalid input (empty steamid) and returns null
		assertNull(result);
	}

	@Test
	void testProcess_EmptyPersonName_ShouldReturnNull() throws JsonProcessingException {
		// Given
		String invalidJson = """
				{
					"profile": {
						"steamid": "76561198012345678",
						"personaname": ""
					}
				}
				""";
		JsonNode playerData = objectMapper.readTree(invalidJson);

		// When
		Player result = playerProcessor.process(playerData);

		// Then - Processor rejects invalid input (empty personaname) and returns null
		assertNull(result);
	}

	@Test
	void testProcess_WithUnixTimestamp_ShouldParseCorrectly() throws JsonProcessingException {
		// Given
		String validJson = """
				{
					"profile": {
						"steamid": "76561198012345678",
						"personaname": "TestPlayer",
						"last_login": "1640995200"
					}
				}
				""";
		JsonNode playerData = objectMapper.readTree(validJson);

		// When
		Player result = playerProcessor.process(playerData);

		// Then
		assertNotNull(result);
		assertNotNull(result.getLastLogin());
		assertEquals(2022, result.getLastLogin().getYear());
		assertEquals(1, result.getLastLogin().getMonthValue());
		assertEquals(1, result.getLastLogin().getDayOfMonth());
	}

	@Test
	void testProcess_WithISODateTime_ShouldParseCorrectly() throws JsonProcessingException {
		// Given
		String validJson = """
				{
					"profile": {
						"steamid": "76561198012345678",
						"personaname": "TestPlayer",
						"last_login": "2022-01-01T00:00:00"
					}
				}
				""";
		JsonNode playerData = objectMapper.readTree(validJson);

		// When
		Player result = playerProcessor.process(playerData);

		// Then
		assertNotNull(result);
		assertNotNull(result.getLastLogin());
		assertEquals(2022, result.getLastLogin().getYear());
		assertEquals(1, result.getLastLogin().getMonthValue());
		assertEquals(1, result.getLastLogin().getDayOfMonth());
	}

	@Test
	void testProcess_WithInvalidDateTime_ShouldHandleGracefully() throws JsonProcessingException {
		// Given
		String validJson = """
				{
					"profile": {
						"steamid": "76561198012345678",
						"personaname": "TestPlayer",
						"last_login": "invalid_date"
					}
				}
				""";
		JsonNode playerData = objectMapper.readTree(validJson);

		// When
		Player result = playerProcessor.process(playerData);

		// Then
		assertNotNull(result);
		assertNull(result.getLastLogin());
	}

	@Test
	void testProcess_WithNullValues_ShouldHandleGracefully() throws JsonProcessingException {
		// Given
		String validJson = """
				{
					"profile": {
						"steamid": "76561198012345678",
						"personaname": "TestPlayer",
						"avatar": null,
						"cheese": null,
						"fh_unavailable": null,
						"plus": null
					},
					"rank_tier": null,
					"leaderboard_rank": null
				}
				""";
		JsonNode playerData = objectMapper.readTree(validJson);

		// When
		Player result = playerProcessor.process(playerData);

		// Then
		assertNotNull(result);
		assertEquals("76561198012345678", result.getSteamId());
		assertEquals("TestPlayer", result.getPersonName());
		assertNull(result.getAvatar());
		assertNull(result.getCheese());
		assertNull(result.getFhUnavailable());
		assertNull(result.getPlus());
		assertNull(result.getRankTier());
		assertNull(result.getLeaderboardRank());
	}

	@Test
	void testProcess_WithWhitespaceValues_ShouldPreserveWhitespace() throws JsonProcessingException {
		// Given
		String validJson = """
				{
					"profile": {
						"steamid": "  76561198012345678  ",
						"personaname": "  TestPlayer  ",
						"loccountrycode": "  US  "
					}
				}
				""";
		JsonNode playerData = objectMapper.readTree(validJson);

		// When
		Player result = playerProcessor.process(playerData);

		// Then - Processor preserves whitespace, doesn't trim
		assertNotNull(result);
		assertEquals("  76561198012345678  ", result.getSteamId());
		assertEquals("  TestPlayer  ", result.getPersonName());
		assertEquals("  US  ", result.getLocCountryCode());
	}

	@Test
	void testProcess_WithEmptyStringValues_ShouldHandleGracefully() throws JsonProcessingException {
		// Given
		String validJson = """
				{
					"profile": {
						"steamid": "76561198012345678",
						"personaname": "TestPlayer",
						"avatar": "",
						"loccountrycode": ""
					}
				}
				""";
		JsonNode playerData = objectMapper.readTree(validJson);

		// When
		Player result = playerProcessor.process(playerData);

		// Then
		assertNotNull(result);
		assertEquals("76561198012345678", result.getSteamId());
		assertEquals("TestPlayer", result.getPersonName());
		assertNull(result.getAvatar());
		assertNull(result.getLocCountryCode());
	}

}
