package com.abe.gg_stats.service;

import com.abe.gg_stats.entity.Player;
import com.abe.gg_stats.repository.PlayerRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlayerUpdateServiceTest {

	@Mock
	private PlayerRepository playerRepository;

	@Mock
	private OpenDotaApiService openDotaApiService;


	private ObjectMapper objectMapper;

	@BeforeEach
	void setUp() {
		playerUpdateService = new PlayerUpdateService(playerRepository, openDotaApiService);
		objectMapper = new ObjectMapper();
	}

	@Test
	void testUpdatePlayerInfo_NewPlayer_ShouldCreateSuccessfully() throws Exception {
		// Given
		Long accountId = 12345L;
		String playerJson = """
				{
					"profile": {
						"steamid": "76561198012345678",
						"personaname": "TestPlayer",
						"avatar": "http://example.com/avatar.jpg",
						"avatarmedium": "http://example.com/avatar_medium.jpg",
						"avatarfull": "http://example.com/avatar_full.jpg",
						"profileurl": "http://steamcommunity.com/profiles/76561198012345678",
						"last_login": "1640995200",
						"full_history_time": "1640995200",
						"last_match_time": "1640995200",
						"cheese": 100,
						"fh_unavailable": false,
						"loccountrycode": "US",
						"plus": true
					},
					"rank_tier": 50,
					"leaderboard_rank": 1000
				}
				""";
		JsonNode playerData = objectMapper.readTree(playerJson);

		when(playerRepository.findById(accountId)).thenReturn(Optional.empty());
		when(openDotaApiService.getPlayer(accountId)).thenReturn(Optional.of(playerData));
		when(playerRepository.save(any(Player.class))).thenAnswer(invocation -> invocation.getArgument(0));

		// When
		Player result = playerUpdateService.updatePlayerInfo(accountId);

		// Then
		assertNotNull(result);
		assertEquals(accountId, result.getAccountId());
		assertEquals("76561198012345678", result.getSteamId());
		assertEquals("TestPlayer", result.getPersonName());
		assertEquals("http://example.com/avatar.jpg", result.getAvatar());
		assertEquals("http://example.com/avatar_medium.jpg", result.getAvatarMedium());
		assertEquals("http://example.com/avatar_full.jpg", result.getAvatarFull());
		assertEquals("http://steamcommunity.com/profiles/76561198012345678", result.getProfileUrl());
		assertEquals(100, result.getCheese());
		assertFalse(result.getFhUnavailable());
		assertEquals("US", result.getLocCountryCode());
		assertTrue(result.getPlus());
		assertEquals(50, result.getRankTier());
		assertEquals(1000, result.getLeaderboardRank());

		verify(playerRepository).save(any(Player.class));
	}

	@Test
	void testUpdatePlayerInfo_ExistingPlayerWithAccurateData_ShouldSkip() {
		// Given
		Long accountId = 12345L;
		Player existingPlayer = new Player();
		existingPlayer.setAccountId(accountId);
		existingPlayer.setPersonName("ExistingPlayer");

		LocalDateTime threeDaysBefore = LocalDateTime.now().minusDays(3);
		existingPlayer.setUpdatedAt(threeDaysBefore);

		when(playerRepository.findById(accountId)).thenReturn(Optional.of(existingPlayer));

		// When
		Player result = playerUpdateService.updatePlayerInfo(accountId);

		// Then
		assertNull(result);
		verify(openDotaApiService, never()).getPlayer(any());
		verify(playerRepository, never()).save(any());
	}

	@Test
	void testUpdatePlayerInfo_ExistingPlayerWithStaleData_ShouldUpdate() throws JsonProcessingException {
		// Given
		Long accountId = 12345L;
		Player existingPlayer = new Player();
		existingPlayer.setAccountId(accountId);
		existingPlayer.setPersonName("ExistingPlayer");
		LocalDateTime twoWeeksBefore = LocalDateTime.now().minusWeeks(2);
		existingPlayer.setUpdatedAt(twoWeeksBefore);
		String playerJson = """
				{
					"profile": {
						"steamid": "12345",
						"personaname": "ExistingPlayer"
					},
					"rank_tier": 50,
					"leaderboard_rank": 1000
				}
				""";
		JsonNode playerData = objectMapper.readTree(playerJson);
		when(playerRepository.findById(accountId)).thenReturn(Optional.of(existingPlayer));
		when(openDotaApiService.getPlayer(accountId)).thenReturn(Optional.of(playerData));
		when(playerRepository.save(any(Player.class))).thenAnswer(invocation -> {
			Player saved = invocation.getArgument(0);
			saved.setUpdatedAt(LocalDateTime.now()); // simulate @UpdateTimestamp
			return saved;
		}); // When

		Player result = playerUpdateService.updatePlayerInfo(accountId);

		// Then
		assertNotNull(result);
		assertTrue(result.getUpdatedAt().isAfter(LocalDateTime.now().minusSeconds(2)));
		verify(openDotaApiService).getPlayer(any());
		verify(playerRepository).save(any());
	}

	@Test
	void testUpdatePlayerInfo_ExistingPlayerWithoutUpdateTimestamp_ShouldUpdate() throws Exception {
		// Given
		Long accountId = 12345L;
		Player existingPlayer = new Player();
		existingPlayer.setAccountId(accountId);
		existingPlayer.setPersonName("ExistingPlayer");

		// No updated_at field
		String playerJson = """
				{
					"profile": {
						"steamid": "76561198012345678",
						"personaname": "UpdatedPlayer"
					}
				}
				""";
		JsonNode playerData = objectMapper.readTree(playerJson);

		when(playerRepository.findById(accountId)).thenReturn(Optional.of(existingPlayer));
		when(openDotaApiService.getPlayer(accountId)).thenReturn(Optional.of(playerData));
		when(playerRepository.save(any(Player.class))).thenAnswer(invocation -> invocation.getArgument(0));

		// When
		Player result = playerUpdateService.updatePlayerInfo(accountId);

		// Then
		assertNotNull(result);
		assertEquals("76561198012345678", result.getSteamId());
		assertEquals("UpdatedPlayer", result.getPersonName());
		verify(playerRepository).save(any(Player.class));
	}

	@Test
	void testUpdatePlayerInfo_NoExistingPlayer_ShouldFetchFromApiAndSave() throws JsonProcessingException {
		// Given
		Long accountId = 12345L;
		String playerJson = """
				{
				    "profile": {
				        "steamid": "12345",
				        "personaname": "NewPlayer"
				    },
				    "rank_tier": 50,
				    "leaderboard_rank": 1000
				}
				""";
		JsonNode playerData = objectMapper.readTree(playerJson);

		when(playerRepository.findById(accountId)).thenReturn(Optional.empty());
		when(openDotaApiService.getPlayer(accountId)).thenReturn(Optional.of(playerData));
		when(playerRepository.save(any(Player.class))).thenAnswer(invocation -> invocation.getArgument(0));

		// When
		Player result = playerUpdateService.updatePlayerInfo(accountId);

		// Then
		assertNotNull(result);
		assertEquals("NewPlayer", result.getPersonName());

		verify(openDotaApiService).getPlayer(accountId);
		verify(playerRepository).save(any(Player.class));
	}

	@Test
	void testUpdatePlayerInfo_NoApiData_ShouldReturnNull() {
		// Given
		Long accountId = 12345L;

		// When
		when(playerRepository.findById(accountId)).thenReturn(Optional.empty());
		when(openDotaApiService.getPlayer(accountId)).thenReturn(Optional.empty());

		// Then
		assertNull(playerUpdateService.updatePlayerInfo(accountId));
	}

	@Test
	void testUpdatePlayerInfo_NoProfileData_ShouldHandleGracefully() throws Exception {
		// Given
		Long accountId = 12345L;
		String playerJsonWithoutProfile = """
				{
					"rank_tier": 50
				}
				""";
		JsonNode playerData = objectMapper.readTree(playerJsonWithoutProfile);

		when(playerRepository.findById(accountId)).thenReturn(Optional.empty());
		when(openDotaApiService.getPlayer(accountId)).thenReturn(Optional.of(playerData));
		when(playerRepository.save(any(Player.class))).thenAnswer(invocation -> invocation.getArgument(0));

		// When
		Player result = playerUpdateService.updatePlayerInfo(accountId);

		// Then
		assertNotNull(result);
		assertEquals(accountId, result.getAccountId());
		assertEquals(50, result.getRankTier());
		// Profile fields should be null
		assertNull(result.getSteamId());
		assertNull(result.getPersonName());
		assertNull(result.getAvatar());
	}

	@Test
	void testUpdatePlayerInfo_UnixTimestampParsing_ShouldSucceed() throws Exception {
		// Given
		Long accountId = 12345L;
		String playerJsonWithUnixTimestamp = """
				{
					"profile": {
						"steamid": "76561198012345678",
						"personaname": "TestPlayer",
						"last_login": "1640995200"
					}
				}
				""";
		JsonNode playerData = objectMapper.readTree(playerJsonWithUnixTimestamp);

		when(playerRepository.findById(accountId)).thenReturn(Optional.empty());
		when(openDotaApiService.getPlayer(accountId)).thenReturn(Optional.of(playerData));
		when(playerRepository.save(any(Player.class))).thenAnswer(invocation -> invocation.getArgument(0));

		// When
		Player result = playerUpdateService.updatePlayerInfo(accountId);

		// Then
		assertNotNull(result);
		assertEquals(accountId, result.getAccountId());
		assertNotNull(result.getLastLogin());
		// 1640995200 should correspond to 2022-01-01 00:00:00 UTC
		assertEquals(2022, result.getLastLogin().getYear());
		assertEquals(1, result.getLastLogin().getMonthValue());
		assertEquals(1, result.getLastLogin().getDayOfMonth());
	}

	@Test
	void testUpdatePlayerInfo_InvalidTimestamp_ShouldHandleGracefully() throws Exception {
		// Given
		Long accountId = 12345L;
		String playerJsonWithInvalidTimestamp = """
				{
					"profile": {
						"steamid": "76561198012345678",
						"personaname": "TestPlayer",
						"last_login": "invalid_timestamp"
					}
				}
				""";
		JsonNode playerData = objectMapper.readTree(playerJsonWithInvalidTimestamp);

		when(playerRepository.findById(accountId)).thenReturn(Optional.empty());
		when(openDotaApiService.getPlayer(accountId)).thenReturn(Optional.of(playerData));
		when(playerRepository.save(any(Player.class))).thenAnswer(invocation -> invocation.getArgument(0));

		// When
		Player result = playerUpdateService.updatePlayerInfo(accountId);

		// Then
		assertNotNull(result);
		assertEquals(accountId, result.getAccountId());
		assertNull(result.getLastLogin()); // Should be null due to parsing failure
	}

	@Test
	void testUpdatePlayerInfo_EmptyStringValues_ShouldHandleGracefully() throws Exception {
		// Given
		Long accountId = 12345L;
		String playerJsonWithEmptyValues = """
				{
					"profile": {
						"steamid": "76561198012345678",
						"personaname": "TestPlayer",
						"loccountrycode": ""
					}
				}
				""";
		JsonNode playerData = objectMapper.readTree(playerJsonWithEmptyValues);

		when(playerRepository.findById(accountId)).thenReturn(Optional.empty());
		when(openDotaApiService.getPlayer(accountId)).thenReturn(Optional.of(playerData));
		when(playerRepository.save(any(Player.class))).thenAnswer(invocation -> invocation.getArgument(0));

		// When
		Player result = playerUpdateService.updatePlayerInfo(accountId);

		// Then
		assertNotNull(result);
		assertEquals("76561198012345678", result.getSteamId());
		assertEquals(accountId, result.getAccountId());
		assertEquals("TestPlayer", result.getPersonName());
		assertNull(result.getLocCountryCode());
	}

	@Test
	void testUpdatePlayerInfo_RepositoryError_ShouldThrowException() {
		// Given
		Long accountId = 12345L;
		when(playerRepository.findById(accountId)).thenThrow(new RuntimeException("Database error"));

		// When & Then
		PlayerUpdateService.PlayerProcessingException exception = assertThrows(
				PlayerUpdateService.PlayerProcessingException.class,
				() -> playerUpdateService.updatePlayerInfo(accountId));
		assertEquals("Failed to update player: " + accountId, exception.getMessage());
		assertNotNull(exception.getCause());
		assertEquals("Database error", exception.getCause().getMessage());
	}

}
