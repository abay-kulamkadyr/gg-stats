package com.abe.gg_stats.batch;

import com.abe.gg_stats.batch.heroRanking.HeroRankingWriter;
import com.abe.gg_stats.entity.HeroRanking;
import com.abe.gg_stats.repository.HeroRankingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.item.Chunk;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HeroRankingWriterTest {

	@Mock
	private HeroRankingRepository heroRankingRepository;

	private HeroRankingWriter writer;

	@BeforeEach
	void setUp() {
		writer = new HeroRankingWriter(heroRankingRepository);
	}

	@Test
	void testWrite_ValidChunk_ShouldSaveAll() throws Exception {
		// Given
		List<HeroRanking> rankings1 = Arrays.asList(createHeroRanking(1, 12345L, 95.5),
				createHeroRanking(1, 67890L, 88.0));

		List<HeroRanking> rankings2 = Arrays.asList(createHeroRanking(2, 11111L, 92.0));

		Chunk<List<HeroRanking>> chunk = new Chunk<>(Arrays.asList(rankings1, rankings2));

		// When
		writer.write(chunk);

		// Then
		verify(heroRankingRepository, times(2)).saveAll(any(List.class));
		verify(heroRankingRepository).saveAll(rankings1);
		verify(heroRankingRepository).saveAll(rankings2);
	}

	@Test
	void testWrite_EmptyChunk_ShouldHandleGracefully() throws Exception {
		// Given
		Chunk<List<HeroRanking>> chunk = new Chunk<>(Collections.emptyList());

		// When
		writer.write(chunk);

		// Then
		verify(heroRankingRepository, never()).saveAll(any(List.class));
	}

	@Test
	void testWrite_NullChunk_ShouldHandleGracefully() {
		// When & Then - Should throw NullPointerException since the method doesn't handle
		// null chunks
		assertThrows(NullPointerException.class, () -> writer.write(null));
		verify(heroRankingRepository, never()).saveAll(any(List.class));
	}

	@Test
	void testWrite_ChunkWithNullItems_ShouldHandleGracefully() throws Exception {
		// Given
		List<HeroRanking> rankings1 = Arrays.asList(createHeroRanking(1, 12345L, 95.5),
				createHeroRanking(1, 67890L, 88.0));

		Chunk<List<HeroRanking>> chunk = new Chunk<>(Arrays.asList(rankings1, null));

		// When - Should handle null item gracefully by catching the exception and
		// continuing
		writer.write(chunk);

		// Then - Should process the valid item and log error for null item
		verify(heroRankingRepository, times(1)).saveAll(rankings1);
	}

	@Test
	void testWrite_SingleItem_ShouldSaveSuccessfully() throws Exception {
		// Given
		List<HeroRanking> rankings = Arrays.asList(createHeroRanking(1, 12345L, 95.5));

		Chunk<List<HeroRanking>> chunk = new Chunk<>(Collections.singletonList(rankings));

		// When
		writer.write(chunk);

		// Then
		verify(heroRankingRepository, times(1)).saveAll(rankings);
	}

	@Test
	void testWrite_DatabaseError_ShouldContinueProcessing() throws Exception {
		// Given
		List<HeroRanking> rankings1 = Arrays.asList(createHeroRanking(1, 12345L, 95.5));

		List<HeroRanking> rankings2 = Arrays.asList(createHeroRanking(2, 67890L, 88.0));

		Chunk<List<HeroRanking>> chunk = new Chunk<>(Arrays.asList(rankings1, rankings2));

		when(heroRankingRepository.saveAll(rankings1)).thenReturn(rankings1);
		when(heroRankingRepository.saveAll(rankings2)).thenThrow(new RuntimeException("Database error"));

		// When - Should not throw exception, just log error
		writer.write(chunk);

		// Then - Should attempt to save both items
		verify(heroRankingRepository).saveAll(rankings1);
		verify(heroRankingRepository).saveAll(rankings2);
	}

	@Test
	void testWrite_PartialFailure_ShouldContinueProcessing() throws Exception {
		// Given
		List<HeroRanking> rankings1 = Arrays.asList(createHeroRanking(1, 12345L, 95.5));

		List<HeroRanking> rankings2 = Arrays.asList(createHeroRanking(2, 67890L, 88.0));

		Chunk<List<HeroRanking>> chunk = new Chunk<>(Arrays.asList(rankings1, rankings2));

		when(heroRankingRepository.saveAll(rankings1)).thenReturn(rankings1);
		when(heroRankingRepository.saveAll(rankings2)).thenThrow(new RuntimeException("Database error"));

		// When - Should not throw exception, just log error for failed item
		writer.write(chunk);

		// Then - Should attempt to save both items
		verify(heroRankingRepository).saveAll(rankings1);
		verify(heroRankingRepository).saveAll(rankings2);
	}

	@Test
	void testWrite_LargeChunk_ShouldHandleCorrectly() throws Exception {
		// Given
		Chunk<List<HeroRanking>> chunk = new Chunk<>();
		for (int i = 0; i < 100; i++) {
			List<HeroRanking> rankings = Arrays.asList(createHeroRanking(i, (long) i, 50.0 + i));
			chunk.add(rankings);
		}

		// When
		writer.write(chunk);

		// Then
		verify(heroRankingRepository, times(100)).saveAll(any(List.class));
	}

	@Test
	void testWrite_ChunkWithMixedData_ShouldHandleCorrectly() throws Exception {
		// Given
		List<HeroRanking> rankings1 = Arrays.asList(createHeroRanking(1, 12345L, 95.5));

		List<HeroRanking> rankings2 = Arrays.asList(createHeroRanking(2, 67890L, null)); // Null
																							// score

		List<HeroRanking> rankings3 = Arrays.asList(createHeroRanking(3, 11111L, 100.0));

		Chunk<List<HeroRanking>> chunk = new Chunk<>(Arrays.asList(rankings1, rankings2, rankings3));

		// When
		writer.write(chunk);

		// Then
		verify(heroRankingRepository, times(3)).saveAll(any(List.class));
		verify(heroRankingRepository).saveAll(rankings1);
		verify(heroRankingRepository).saveAll(rankings2);
		verify(heroRankingRepository).saveAll(rankings3);
	}

	@Test
	void testWrite_ChunkWithZeroValues_ShouldHandleCorrectly() throws Exception {
		// Given
		List<HeroRanking> rankings = Arrays.asList(createHeroRanking(0, 0L, 0.0));

		Chunk<List<HeroRanking>> chunk = new Chunk<>(Collections.singletonList(rankings));

		// When
		writer.write(chunk);

		// Then
		verify(heroRankingRepository, times(1)).saveAll(rankings);
	}

	@Test
	void testWrite_ChunkWithMaxValues_ShouldHandleCorrectly() throws Exception {
		// Given
		List<HeroRanking> rankings = List.of(createHeroRanking(Integer.MAX_VALUE, Long.MAX_VALUE, Double.MAX_VALUE));

		Chunk<List<HeroRanking>> chunk = new Chunk<>(Collections.singletonList(rankings));

		// When
		writer.write(chunk);

		// Then
		verify(heroRankingRepository, times(1)).saveAll(rankings);
	}

	@Test
	void testWrite_ChunkWithNegativeValues_ShouldHandleCorrectly() throws Exception {
		// Given
		List<HeroRanking> rankings = Arrays.asList(createHeroRanking(-1, -12345L, -50.0));

		Chunk<List<HeroRanking>> chunk = new Chunk<>(Collections.singletonList(rankings));

		// When
		writer.write(chunk);

		// Then
		verify(heroRankingRepository, times(1)).saveAll(rankings);
	}

	@Test
	void testWrite_EmptyRankingsList_ShouldHandleCorrectly() throws Exception {
		// Given
		List<HeroRanking> emptyRankings = Collections.emptyList();
		Chunk<List<HeroRanking>> chunk = new Chunk<>(Collections.singletonList(emptyRankings));

		// When
		writer.write(chunk);

		// Then
		verify(heroRankingRepository, never()).saveAll(any(List.class)); // Should not
																			// call
																			// saveAll for
																			// empty list
	}

	@Test
	void testWrite_MultipleRankingsInSingleList_ShouldSaveAll() throws Exception {
		// Given
		List<HeroRanking> multipleRankings = Arrays.asList(createHeroRanking(1, 12345L, 95.5),
				createHeroRanking(1, 67890L, 88.0), createHeroRanking(1, 11111L, 92.0));

		Chunk<List<HeroRanking>> chunk = new Chunk<>(Collections.singletonList(multipleRankings));

		// When
		writer.write(chunk);

		// Then
		verify(heroRankingRepository, times(1)).saveAll(multipleRankings);
	}

	private HeroRanking createHeroRanking(Integer heroId, Long accountId, Double score) {
		HeroRanking ranking = new HeroRanking();
		ranking.setHeroId(heroId);
		ranking.setAccountId(accountId);
		ranking.setScore(score);
		return ranking;
	}

}
