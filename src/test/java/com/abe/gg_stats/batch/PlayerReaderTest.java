package com.abe.gg_stats.batch;

import com.abe.gg_stats.entity.HeroRanking;
import com.abe.gg_stats.entity.NotablePlayer;
import com.abe.gg_stats.entity.Player;
import com.abe.gg_stats.repository.HeroRankingRepository;
import com.abe.gg_stats.repository.NotablePlayerRepository;
import com.abe.gg_stats.repository.PlayerRepository;
import com.abe.gg_stats.service.OpenDotaApiService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlayerReaderTest {

	@Mock
	private HeroRankingRepository heroRankingRepository;

	@Mock
	private NotablePlayerRepository notablePlayerRepository;

	@Mock
	private PlayerRepository playerRepository;

	@Mock
	private OpenDotaApiService openDotaApiService;

	private PlayerReader reader;

	@BeforeEach
	void setUp() {
		reader = new PlayerReader(heroRankingRepository, notablePlayerRepository, playerRepository, openDotaApiService);
	}

	@Test
	void testRead_FirstCall_ShouldInitializeAndReturnFirstAccountId() {
		// Given
		HeroRanking ranking1 = new HeroRanking();
		ranking1.setAccountId(12345L);

		HeroRanking ranking2 = new HeroRanking();
		ranking2.setAccountId(67890L);

		NotablePlayer player1 = new NotablePlayer();
		player1.setAccountId(11111L);

		Player player2 = new Player();
		player2.setAccountId(22222L);

		when(heroRankingRepository.findAll()).thenReturn(Arrays.asList(ranking1, ranking2));
		when(notablePlayerRepository.findAll()).thenReturn(Arrays.asList(player1));
		when(playerRepository.findAll()).thenReturn(Arrays.asList(player2));

		// When
		Long result = reader.read();

		// Then
		assertNotNull(result);
		// Should return one of the account IDs (order depends on HashSet iteration)
		assertTrue(Arrays.asList(12345L, 67890L, 11111L, 22222L).contains(result));

		verify(heroRankingRepository).findAll();
		verify(notablePlayerRepository).findAll();
		verify(playerRepository).findAll();
	}

	@Test
	void testRead_SubsequentCalls_ShouldReturnNextAccountIds() {
		// Given
		HeroRanking ranking1 = new HeroRanking();
		ranking1.setAccountId(12345L);

		HeroRanking ranking2 = new HeroRanking();
		ranking2.setAccountId(67890L);

		when(heroRankingRepository.findAll()).thenReturn(Arrays.asList(ranking1, ranking2));
		when(notablePlayerRepository.findAll()).thenReturn(Arrays.asList());
		when(playerRepository.findAll()).thenReturn(Arrays.asList());

		// When - First call initializes and returns first ID
		Long firstResult = reader.read();
		// Second call should return second ID
		Long secondResult = reader.read();
		// Third call should return null (end of data)
		Long thirdResult = reader.read();

		// Then
		assertNotNull(firstResult);
		assertNotNull(secondResult);
		assertNull(thirdResult);
		// Order in HashSet is not guaranteed, so check both values
		assertTrue(Arrays.asList(12345L, 67890L).contains(firstResult));
		assertTrue(Arrays.asList(12345L, 67890L).contains(secondResult));
		assertNotEquals(firstResult, secondResult);

		verify(heroRankingRepository, times(1)).findAll();
		verify(notablePlayerRepository, times(1)).findAll();
		verify(playerRepository, times(1)).findAll();
	}

	@Test
	void testRead_AfterAllItemsRead_ShouldReturnNull() {
		// Given
		HeroRanking ranking = new HeroRanking();
		ranking.setAccountId(12345L);

		when(heroRankingRepository.findAll()).thenReturn(Arrays.asList(ranking));
		when(notablePlayerRepository.findAll()).thenReturn(Arrays.asList());
		when(playerRepository.findAll()).thenReturn(Arrays.asList());

		// When
		Long firstResult = reader.read();
		Long secondResult = reader.read();

		// Then
		assertNotNull(firstResult);
		assertNull(secondResult); // Should return null after all items read (only one
									// unique ID)
	}

	@Test
	void testRead_WithDuplicateAccountIds_ShouldReturnUniqueIds() {
		// Given
		HeroRanking ranking1 = new HeroRanking();
		ranking1.setAccountId(12345L);

		HeroRanking ranking2 = new HeroRanking();
		ranking2.setAccountId(12345L); // Duplicate

		NotablePlayer player = new NotablePlayer();
		player.setAccountId(12345L); // Duplicate

		when(heroRankingRepository.findAll()).thenReturn(Arrays.asList(ranking1, ranking2));
		when(notablePlayerRepository.findAll()).thenReturn(Arrays.asList(player));
		when(playerRepository.findAll()).thenReturn(Arrays.asList());

		// When
		Long firstResult = reader.read();
		Long secondResult = reader.read();

		// Then
		assertNotNull(firstResult);
		assertEquals(12345L, firstResult);
		assertNull(secondResult); // Only one unique ID should be returned
	}

	@Test
	void testRead_WithNullAccountIds_ShouldFilterOutNulls() {
		// Given
		HeroRanking ranking1 = new HeroRanking();
		ranking1.setAccountId(12345L);

		HeroRanking ranking2 = new HeroRanking();
		ranking2.setAccountId(null); // Null account ID

		NotablePlayer player = new NotablePlayer();
		player.setAccountId(null); // Null account ID

		when(heroRankingRepository.findAll()).thenReturn(Arrays.asList(ranking1, ranking2));
		when(notablePlayerRepository.findAll()).thenReturn(Arrays.asList(player));
		when(playerRepository.findAll()).thenReturn(Arrays.asList());

		// When
		Long result = reader.read();
		Long secondResult = reader.read();

		// Then
		assertNotNull(result);
		assertEquals(12345L, result);
		assertNull(secondResult); // Only one valid ID should be returned
	}

	@Test
	void testRead_WithEmptyRepositories_ShouldReturnNull() {
		// Given
		when(heroRankingRepository.findAll()).thenReturn(Arrays.asList());
		when(notablePlayerRepository.findAll()).thenReturn(Arrays.asList());
		when(playerRepository.findAll()).thenReturn(Arrays.asList());

		// When
		Long result = reader.read();

		// Then
		assertNull(result);
	}

	@Test
	void testRead_WithMixedData_ShouldCollectAllValidAccountIds() {
		// Given
		HeroRanking ranking1 = new HeroRanking();
		ranking1.setAccountId(12345L);

		HeroRanking ranking2 = new HeroRanking();
		ranking2.setAccountId(67890L);

		NotablePlayer player1 = new NotablePlayer();
		player1.setAccountId(11111L);

		NotablePlayer player2 = new NotablePlayer();
		player2.setAccountId(null); // Invalid

		Player player3 = new Player();
		player3.setAccountId(22222L);

		when(heroRankingRepository.findAll()).thenReturn(Arrays.asList(ranking1, ranking2));
		when(notablePlayerRepository.findAll()).thenReturn(Arrays.asList(player1, player2));
		when(playerRepository.findAll()).thenReturn(Arrays.asList(player3));

		// When
		Long firstResult = reader.read();
		Long secondResult = reader.read();
		Long thirdResult = reader.read();
		Long fourthResult = reader.read();
		Long fifthResult = reader.read();

		// Then
		assertNotNull(firstResult);
		assertNotNull(secondResult);
		assertNotNull(thirdResult);
		assertNotNull(fourthResult);
		assertNull(fifthResult); // Should return null after all 4 valid IDs

		// Verify all valid account IDs were collected
		List<Long> expectedIds = Arrays.asList(12345L, 67890L, 11111L, 22222L);
		assertTrue(expectedIds.contains(firstResult));
		assertTrue(expectedIds.contains(secondResult));
		assertTrue(expectedIds.contains(thirdResult));
		assertTrue(expectedIds.contains(fourthResult));
	}

	@Test
	void testRead_RepositoryException_ShouldThrowCustomException() {
		// Given
		when(heroRankingRepository.findAll()).thenThrow(new RuntimeException("Database error"));

		// When & Then
		PlayerReader.PlayerReadException exception = assertThrows(PlayerReader.PlayerReadException.class,
				() -> reader.read());

		assertEquals("Failed to collect account IDs", exception.getMessage());
		assertNotNull(exception.getCause());
		assertEquals("Database error", exception.getCause().getMessage());
	}

	@Test
	void testRead_WithLargeDataset_ShouldHandleCorrectly() {
		// Given
		List<HeroRanking> rankings = new ArrayList<>();
		for (int i = 0; i < 1000; i++) {
			HeroRanking ranking = new HeroRanking();
			ranking.setAccountId((long) i);
			rankings.add(ranking);
		}

		when(heroRankingRepository.findAll()).thenReturn(rankings);
		when(notablePlayerRepository.findAll()).thenReturn(Arrays.asList());
		when(playerRepository.findAll()).thenReturn(Arrays.asList());

		// When
		Long result = reader.read();

		// Then
		assertNotNull(result);
		assertTrue(result >= 0 && result < 1000);
	}

	@Test
	void testRead_WithNegativeAccountIds_ShouldHandleCorrectly() {
		// Given
		HeroRanking ranking1 = new HeroRanking();
		ranking1.setAccountId(-12345L);

		HeroRanking ranking2 = new HeroRanking();
		ranking2.setAccountId(67890L);

		when(heroRankingRepository.findAll()).thenReturn(Arrays.asList(ranking1, ranking2));
		when(notablePlayerRepository.findAll()).thenReturn(Arrays.asList());
		when(playerRepository.findAll()).thenReturn(Arrays.asList());

		// When
		Long firstResult = reader.read();
		Long secondResult = reader.read();
		Long thirdResult = reader.read();

		// Then
		assertNotNull(firstResult);
		assertNotNull(secondResult);
		assertNull(thirdResult); // Should return null after all valid IDs

		// Both negative and positive IDs should be collected
		assertTrue(Arrays.asList(-12345L, 67890L).contains(firstResult));
		assertTrue(Arrays.asList(-12345L, 67890L).contains(secondResult));
	}

	@Test
	void testRead_WithZeroAccountIds_ShouldHandleCorrectly() {
		// Given
		HeroRanking ranking1 = new HeroRanking();
		ranking1.setAccountId(0L);

		HeroRanking ranking2 = new HeroRanking();
		ranking2.setAccountId(12345L);

		when(heroRankingRepository.findAll()).thenReturn(Arrays.asList(ranking1, ranking2));
		when(notablePlayerRepository.findAll()).thenReturn(Arrays.asList());
		when(playerRepository.findAll()).thenReturn(Arrays.asList());

		// When
		Long firstResult = reader.read();
		Long secondResult = reader.read();
		Long thirdResult = reader.read();

		// Then
		assertNotNull(firstResult);
		assertNotNull(secondResult);
		assertNull(thirdResult); // Should return null after all valid IDs

		// Both zero and positive IDs should be collected
		assertTrue(Arrays.asList(0L, 12345L).contains(firstResult));
		assertTrue(Arrays.asList(0L, 12345L).contains(secondResult));
	}

}
