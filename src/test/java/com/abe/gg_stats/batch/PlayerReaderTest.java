package com.abe.gg_stats.batch;

import com.abe.gg_stats.batch.player.PlayerReader;
import com.abe.gg_stats.config.BatchExpirationConfig;
import com.abe.gg_stats.entity.HeroRanking;
import com.abe.gg_stats.entity.NotablePlayer;
import com.abe.gg_stats.entity.Player;
import com.abe.gg_stats.repository.HeroRankingRepository;
import com.abe.gg_stats.repository.NotablePlayerRepository;
import com.abe.gg_stats.repository.PlayerRepository;
import com.abe.gg_stats.service.OpenDotaApiService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PlayerReaderTest {

	@Mock
	private HeroRankingRepository heroRankingRepository;

	@Mock
	private NotablePlayerRepository notablePlayerRepository;

	@Mock
	private PlayerRepository playerRepository;

	@Mock
	private OpenDotaApiService openDotaApiService;

	@Mock
	private BatchExpirationConfig batchExpirationConfig;

	private PlayerReader reader;

	private ObjectMapper objectMapper;

	@BeforeEach
	void setUp() {
		reader = new PlayerReader(openDotaApiService, batchExpirationConfig, heroRankingRepository,
				notablePlayerRepository, playerRepository);
		objectMapper = new ObjectMapper();
	}

	@Test
	void testRead_FirstCall_ShouldInitializeAndReturnFirstPlayerData() throws Exception {
		// Given
		HeroRanking ranking1 = new HeroRanking();
		ranking1.setAccountId(12345L);

		HeroRanking ranking2 = new HeroRanking();
		ranking2.setAccountId(67890L);

		NotablePlayer player1 = new NotablePlayer();
		player1.setAccountId(11111L);

		Player player2 = new Player();
		player2.setAccountId(22222L);
		player2.setUpdatedAt(LocalDateTime.now().minusDays(1)); // Old data

		when(heroRankingRepository.findAll()).thenReturn(Arrays.asList(ranking1, ranking2));
		when(notablePlayerRepository.findAll()).thenReturn(Arrays.asList(player1));
		when(playerRepository.findAll()).thenReturn(Arrays.asList(player2));

		// Mock API responses for all account IDs that will be processed
		String playerJson1 = """
				{
					"account_id": 12345,
					"profile": {
						"steamid": "76561198012345678",
						"personaname": "TestPlayer1"
					}
				}
				""";
		String playerJson2 = """
				{
					"account_id": 67890,
					"profile": {
						"steamid": "76561198087654321",
						"personaname": "TestPlayer2"
					}
				}
				""";
		String playerJson3 = """
				{
					"account_id": 11111,
					"profile": {
						"steamid": "76561198011111111",
						"personaname": "TestPlayer3"
					}
				}
				""";
		String playerJson4 = """
				{
					"account_id": 22222,
					"profile": {
						"steamid": "76561198022222222",
						"personaname": "TestPlayer4"
					}
				}
				""";

		JsonNode playerData1 = objectMapper.readTree(playerJson1);
		JsonNode playerData2 = objectMapper.readTree(playerJson2);
		JsonNode playerData3 = objectMapper.readTree(playerJson3);
		JsonNode playerData4 = objectMapper.readTree(playerJson4);

		when(openDotaApiService.getPlayer(12345L)).thenReturn(Optional.of(playerData1));
		when(openDotaApiService.getPlayer(67890L)).thenReturn(Optional.of(playerData2));
		when(openDotaApiService.getPlayer(11111L)).thenReturn(Optional.of(playerData3));
		when(openDotaApiService.getPlayer(22222L)).thenReturn(Optional.of(playerData4));

		// Mock expiration check
		when(batchExpirationConfig.getDurationByConfigName("players")).thenReturn(java.time.Duration.ofDays(1));

		// When
		JsonNode result = reader.read();

		// Then
		assertNotNull(result);
		assertTrue(result.has("account_id"));
		assertTrue(result.has("profile"));

		verify(heroRankingRepository).findAll();
		verify(notablePlayerRepository).findAll();
		verify(playerRepository).findAll();
		verify(openDotaApiService).getPlayer(12345L);
		verify(openDotaApiService).getPlayer(67890L);
		verify(openDotaApiService).getPlayer(11111L);
		verify(openDotaApiService).getPlayer(22222L);
	}

	@Test
	void testRead_SubsequentCalls_ShouldReturnNextPlayerData() throws Exception {
		// Given
		HeroRanking ranking1 = new HeroRanking();
		ranking1.setAccountId(12345L);

		HeroRanking ranking2 = new HeroRanking();
		ranking2.setAccountId(67890L);

		when(heroRankingRepository.findAll()).thenReturn(Arrays.asList(ranking1, ranking2));
		when(notablePlayerRepository.findAll()).thenReturn(Arrays.asList());
		when(playerRepository.findAll()).thenReturn(Arrays.asList());

		// Mock API responses
		String player1Json = """
				{
					"account_id": 12345,
					"profile": {
						"steamid": "76561198012345678",
						"personaname": "TestPlayer1"
					}
				}
				""";
		String player2Json = """
				{
					"account_id": 67890,
					"profile": {
						"steamid": "76561198087654321",
						"personaname": "TestPlayer2"
					}
				}
				""";
		JsonNode player1Data = objectMapper.readTree(player1Json);
		JsonNode player2Data = objectMapper.readTree(player2Json);

		when(openDotaApiService.getPlayer(12345L)).thenReturn(Optional.of(player1Data));
		when(openDotaApiService.getPlayer(67890L)).thenReturn(Optional.of(player2Data));

		// Mock expiration check
		when(batchExpirationConfig.getDurationByConfigName("players")).thenReturn(java.time.Duration.ofDays(1));

		// When - First call initializes and returns first player
		JsonNode firstResult = reader.read();
		// Second call should return second player
		JsonNode secondResult = reader.read();
		// Third call should return null (end of data)
		JsonNode thirdResult = reader.read();

		// Then
		assertNotNull(firstResult);
		assertNotNull(secondResult);
		assertNull(thirdResult);

		verify(openDotaApiService).getPlayer(12345L);
		verify(openDotaApiService).getPlayer(67890L);
	}

	@Test
	void testRead_WithFreshData_ShouldSkipApiCall() throws Exception {
		// Given
		Player existingPlayer = new Player();
		existingPlayer.setAccountId(12345L);
		existingPlayer.setUpdatedAt(LocalDateTime.now()); // Fresh data

		when(heroRankingRepository.findAll()).thenReturn(Arrays.asList());
		when(notablePlayerRepository.findAll()).thenReturn(Arrays.asList());
		when(playerRepository.findAll()).thenReturn(Arrays.asList(existingPlayer));

		// Mock the findById call to return the existing player
		when(playerRepository.findById(12345L)).thenReturn(Optional.of(existingPlayer));

		// Mock expiration check - data is fresh
		when(batchExpirationConfig.getDurationByConfigName("players")).thenReturn(java.time.Duration.ofDays(1));

		// When
		JsonNode result = reader.read();

		// Then
		assertNull(result); // Should return null as no API calls needed
		verify(openDotaApiService, never()).getPlayer(any());
	}

	@Test
	void testRead_WithNoApiResponse_ShouldSkipPlayer() throws Exception {
		// Given
		HeroRanking ranking = new HeroRanking();
		ranking.setAccountId(12345L);

		when(heroRankingRepository.findAll()).thenReturn(Arrays.asList(ranking));
		when(notablePlayerRepository.findAll()).thenReturn(Arrays.asList());
		when(playerRepository.findAll()).thenReturn(Arrays.asList());

		// Mock API response - no data
		when(openDotaApiService.getPlayer(12345L)).thenReturn(Optional.empty());

		// Mock expiration check
		when(batchExpirationConfig.getDurationByConfigName("players")).thenReturn(java.time.Duration.ofDays(1));

		// When
		JsonNode result = reader.read();

		// Then
		assertNull(result); // Should return null as no valid data
		verify(openDotaApiService).getPlayer(12345L);
	}

	@Test
	void testRead_WithEmptyRepositories_ShouldReturnNull() throws Exception {
		// Given
		when(heroRankingRepository.findAll()).thenReturn(Arrays.asList());
		when(notablePlayerRepository.findAll()).thenReturn(Arrays.asList());
		when(playerRepository.findAll()).thenReturn(Arrays.asList());

		// When
		JsonNode result = reader.read();

		// Then
		assertNull(result); // Should return null when no data available

		verify(heroRankingRepository).findAll();
		verify(notablePlayerRepository).findAll();
		verify(playerRepository).findAll();
		verify(openDotaApiService, never()).getPlayer(any());
	}

	@Test
	void testRead_WithDuplicateAccountIds_ShouldProcessUniqueIds() throws Exception {
		// Given
		HeroRanking ranking1 = new HeroRanking();
		ranking1.setAccountId(12345L);

		HeroRanking ranking2 = new HeroRanking();
		ranking2.setAccountId(12345L); // Duplicate

		NotablePlayer player1 = new NotablePlayer();
		player1.setAccountId(12345L); // Another duplicate

		when(heroRankingRepository.findAll()).thenReturn(Arrays.asList(ranking1, ranking2));
		when(notablePlayerRepository.findAll()).thenReturn(Arrays.asList(player1));
		when(playerRepository.findAll()).thenReturn(Arrays.asList());

		// Mock API response
		String playerJson = """
				{
					"account_id": 12345,
					"profile": {
						"steamid": "76561198012345678",
						"personaname": "TestPlayer"
					}
				}
				""";
		JsonNode playerData = objectMapper.readTree(playerJson);
		when(openDotaApiService.getPlayer(12345L)).thenReturn(Optional.of(playerData));

		// Mock expiration check
		when(batchExpirationConfig.getDurationByConfigName("players")).thenReturn(java.time.Duration.ofDays(1));

		// When
		JsonNode firstResult = reader.read();
		JsonNode secondResult = reader.read();

		// Then
		assertNotNull(firstResult);
		assertNull(secondResult); // Should return null after all unique IDs processed

		verify(openDotaApiService, times(1)).getPlayer(12345L); // Should only call once
																// for unique ID
	}

	@Test
	void testRead_WithNullAccountIds_ShouldFilterOutNulls() throws Exception {
		// Given
		HeroRanking ranking1 = new HeroRanking();
		ranking1.setAccountId(12345L);

		HeroRanking ranking2 = new HeroRanking();
		ranking2.setAccountId(null); // Null account ID

		NotablePlayer player1 = new NotablePlayer();
		player1.setAccountId(67890L);

		Player player2 = new Player();
		player2.setAccountId(null); // Null account ID

		when(heroRankingRepository.findAll()).thenReturn(Arrays.asList(ranking1, ranking2));
		when(notablePlayerRepository.findAll()).thenReturn(Arrays.asList(player1));
		when(playerRepository.findAll()).thenReturn(Arrays.asList(player2));

		// Mock API responses
		String player1Json = """
				{
					"account_id": 12345,
					"profile": {
						"steamid": "76561198012345678",
						"personaname": "TestPlayer1"
					}
				}
				""";
		String player2Json = """
				{
					"account_id": 67890,
					"profile": {
						"steamid": "76561198087654321",
						"personaname": "TestPlayer2"
					}
				}
				""";
		JsonNode player1Data = objectMapper.readTree(player1Json);
		JsonNode player2Data = objectMapper.readTree(player2Json);

		when(openDotaApiService.getPlayer(12345L)).thenReturn(Optional.of(player1Data));
		when(openDotaApiService.getPlayer(67890L)).thenReturn(Optional.of(player2Data));

		// Mock expiration check
		when(batchExpirationConfig.getDurationByConfigName("players")).thenReturn(java.time.Duration.ofDays(1));

		// When
		JsonNode firstResult = reader.read();
		JsonNode secondResult = reader.read();
		JsonNode thirdResult = reader.read();

		// Then
		assertNotNull(firstResult);
		assertNotNull(secondResult);
		assertNull(thirdResult); // Should return null after all valid IDs processed

		verify(openDotaApiService).getPlayer(12345L);
		verify(openDotaApiService).getPlayer(67890L);
	}

	@Test
	void testRead_WithLargeDataset_ShouldHandleCorrectly() throws Exception {
		// Given
		List<HeroRanking> rankings = new ArrayList<>();
		for (int i = 1; i <= 10; i++) {
			HeroRanking ranking = new HeroRanking();
			ranking.setAccountId((long) i);
			rankings.add(ranking);
		}

		when(heroRankingRepository.findAll()).thenReturn(rankings);
		when(notablePlayerRepository.findAll()).thenReturn(Arrays.asList());
		when(playerRepository.findAll()).thenReturn(Arrays.asList());

		// Mock API responses for all players
		for (int i = 1; i <= 10; i++) {
			String playerJson = String.format("""
					{
						"account_id": %d,
						"profile": {
							"steamid": "76561198%08d",
							"personaname": "TestPlayer%d"
						}
					}
					""", i, i, i);
			JsonNode playerData = objectMapper.readTree(playerJson);
			when(openDotaApiService.getPlayer((long) i)).thenReturn(Optional.of(playerData));
		}

		// Mock expiration check
		when(batchExpirationConfig.getDurationByConfigName("players")).thenReturn(java.time.Duration.ofDays(1));

		// When
		int count = 0;
		JsonNode result;
		while ((result = reader.read()) != null) {
			count++;
			assertTrue(result.has("account_id"));
			assertTrue(result.has("profile"));
		}

		// Then
		assertEquals(10, count);

		verify(heroRankingRepository).findAll();
		verify(notablePlayerRepository).findAll();
		verify(playerRepository).findAll();
		for (int i = 1; i <= 10; i++) {
			verify(openDotaApiService).getPlayer((long) i);
		}
	}

	@Test
	void testRead_WithExpiredData_ShouldFetchFromApi() throws Exception {
		// Given
		Player existingPlayer = new Player();
		existingPlayer.setAccountId(12345L);
		existingPlayer.setUpdatedAt(LocalDateTime.now().minusDays(2)); // Expired data

		when(heroRankingRepository.findAll()).thenReturn(Arrays.asList());
		when(notablePlayerRepository.findAll()).thenReturn(Arrays.asList());
		when(playerRepository.findAll()).thenReturn(Arrays.asList(existingPlayer));

		// Mock API response
		String playerJson = """
				{
					"account_id": 12345,
					"profile": {
						"steamid": "76561198012345678",
						"personaname": "TestPlayer"
					}
				}
				""";
		JsonNode playerData = objectMapper.readTree(playerJson);
		when(openDotaApiService.getPlayer(12345L)).thenReturn(Optional.of(playerData));

		// Mock expiration check - data is expired
		when(batchExpirationConfig.getDurationByConfigName("players")).thenReturn(java.time.Duration.ofDays(1));

		// When
		JsonNode result = reader.read();

		// Then
		assertNotNull(result);
		verify(openDotaApiService).getPlayer(12345L);
	}

}
