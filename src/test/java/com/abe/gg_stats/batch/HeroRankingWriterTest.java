package com.abe.gg_stats.batch;

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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

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
		HeroRanking ranking1 = new HeroRanking();
		ranking1.setHeroId(1);
		ranking1.setAccountId(12345L);
		ranking1.setScore(95.5);

		HeroRanking ranking2 = new HeroRanking();
		ranking2.setHeroId(2);
		ranking2.setAccountId(67890L);
		ranking2.setScore(88.0);

		Chunk<HeroRanking> chunk = new Chunk<>(Arrays.asList(ranking1, ranking2));

		// When
		writer.write(chunk);

		// Then
		verify(heroRankingRepository, times(2)).save(any(HeroRanking.class));
		verify(heroRankingRepository).save(ranking1);
		verify(heroRankingRepository).save(ranking2);
	}

	@Test
	void testWrite_EmptyChunk_ShouldHandleGracefully() throws Exception {
		// Given
		Chunk<HeroRanking> chunk = new Chunk<>(Collections.emptyList());

		// When
		writer.write(chunk);

		// Then
		verify(heroRankingRepository, never()).save(any(HeroRanking.class));
	}

	@Test
	void testWrite_NullChunk_ShouldHandleGracefully() throws Exception {
		// When
		writer.write(null);

		// Then
		verify(heroRankingRepository, never()).save(any(HeroRanking.class));
	}

	@Test
	void testWrite_ChunkWithNullItems_ShouldSkipNulls() throws Exception {
		// Given
		HeroRanking ranking1 = new HeroRanking();
		ranking1.setHeroId(1);
		ranking1.setAccountId(12345L);
		ranking1.setScore(95.5);

		Chunk<HeroRanking> chunk = new Chunk<>(Arrays.asList(ranking1, null));

		// When
		writer.write(chunk);

		// Then
		verify(heroRankingRepository, times(1)).save(any(HeroRanking.class));
		verify(heroRankingRepository).save(ranking1);
	}

	@Test
	void testWrite_SingleItem_ShouldSaveSuccessfully() throws Exception {
		// Given
		HeroRanking ranking = new HeroRanking();
		ranking.setHeroId(1);
		ranking.setAccountId(12345L);
		ranking.setScore(95.5);

		Chunk<HeroRanking> chunk = new Chunk<>(Collections.singletonList(ranking));

		// When
		writer.write(chunk);

		// Then
		verify(heroRankingRepository, times(1)).save(ranking);
	}

	@Test
	void testWrite_RepositoryException_ShouldThrowCustomException() {
		// Given
		HeroRanking ranking = new HeroRanking();
		ranking.setHeroId(1);
		ranking.setAccountId(12345L);
		ranking.setScore(95.5);

		Chunk<HeroRanking> chunk = new Chunk<>(Collections.singletonList(ranking));

		when(heroRankingRepository.save(ranking)).thenThrow(new RuntimeException("Database error"));

		// When & Then
		HeroRankingWriter.HeroRankingWriteException exception = assertThrows(
				HeroRankingWriter.HeroRankingWriteException.class, () -> writer.write(chunk));

		assertEquals("Failed to write hero ranking", exception.getMessage());
		assertNotNull(exception.getCause());
		assertEquals("Database error", exception.getCause().getMessage());

		verify(heroRankingRepository).save(ranking);
	}

	@Test
	void testWrite_PartialFailure_ShouldThrowExceptionOnFirstError() {
		// Given
		HeroRanking ranking1 = new HeroRanking();
		ranking1.setHeroId(1);
		ranking1.setAccountId(12345L);
		ranking1.setScore(95.5);

		HeroRanking ranking2 = new HeroRanking();
		ranking2.setHeroId(2);
		ranking2.setAccountId(67890L);
		ranking2.setScore(88.0);

		Chunk<HeroRanking> chunk = new Chunk<>(Arrays.asList(ranking1, ranking2));

		when(heroRankingRepository.save(ranking1)).thenReturn(ranking1);
		when(heroRankingRepository.save(ranking2)).thenThrow(new RuntimeException("Database error"));

		// When & Then
		HeroRankingWriter.HeroRankingWriteException exception = assertThrows(
				HeroRankingWriter.HeroRankingWriteException.class, () -> writer.write(chunk));

		assertEquals("Failed to write hero ranking", exception.getMessage());
		assertNotNull(exception.getCause());
		assertEquals("Database error", exception.getCause().getMessage());

		verify(heroRankingRepository).save(ranking1);
		verify(heroRankingRepository).save(ranking2);
	}

	@Test
	void testWrite_LargeChunk_ShouldHandleCorrectly() throws Exception {
		// Given
		Chunk<HeroRanking> chunk = new Chunk<>();
		for (int i = 0; i < 100; i++) {
			HeroRanking ranking = new HeroRanking();
			ranking.setHeroId(i);
			ranking.setAccountId((long) i);
			ranking.setScore(50.0 + i);
			chunk.add(ranking);
		}

		// When
		writer.write(chunk);

		// Then
		verify(heroRankingRepository, times(100)).save(any(HeroRanking.class));
	}

	@Test
	void testWrite_ChunkWithMixedData_ShouldHandleCorrectly() throws Exception {
		// Given
		HeroRanking ranking1 = new HeroRanking();
		ranking1.setHeroId(1);
		ranking1.setAccountId(12345L);
		ranking1.setScore(95.5);

		HeroRanking ranking2 = new HeroRanking();
		ranking2.setHeroId(2);
		ranking2.setAccountId(67890L);
		ranking2.setScore(null); // Null score

		HeroRanking ranking3 = new HeroRanking();
		ranking3.setHeroId(3);
		ranking3.setAccountId(11111L);
		ranking3.setScore(100.0);

		Chunk<HeroRanking> chunk = new Chunk<>(Arrays.asList(ranking1, ranking2, ranking3));

		// When
		writer.write(chunk);

		// Then
		verify(heroRankingRepository, times(3)).save(any(HeroRanking.class));
		verify(heroRankingRepository).save(ranking1);
		verify(heroRankingRepository).save(ranking2);
		verify(heroRankingRepository).save(ranking3);
	}

	@Test
	void testWrite_ChunkWithZeroValues_ShouldHandleCorrectly() throws Exception {
		// Given
		HeroRanking ranking = new HeroRanking();
		ranking.setHeroId(0);
		ranking.setAccountId(0L);
		ranking.setScore(0.0);

		Chunk<HeroRanking> chunk = new Chunk<>(Collections.singletonList(ranking));

		// When
		writer.write(chunk);

		// Then
		verify(heroRankingRepository, times(1)).save(ranking);
	}

	@Test
	void testWrite_ChunkWithMaxValues_ShouldHandleCorrectly() throws Exception {
		// Given
		HeroRanking ranking = new HeroRanking();
		ranking.setHeroId(Integer.MAX_VALUE);
		ranking.setAccountId(Long.MAX_VALUE);
		ranking.setScore(Double.MAX_VALUE);

		Chunk<HeroRanking> chunk = new Chunk<>(Collections.singletonList(ranking));

		// When
		writer.write(chunk);

		// Then
		verify(heroRankingRepository, times(1)).save(ranking);
	}

	@Test
	void testWrite_ChunkWithNegativeValues_ShouldHandleCorrectly() throws Exception {
		// Given
		HeroRanking ranking = new HeroRanking();
		ranking.setHeroId(-1);
		ranking.setAccountId(-12345L);
		ranking.setScore(-50.0);

		Chunk<HeroRanking> chunk = new Chunk<>(Collections.singletonList(ranking));

		// When
		writer.write(chunk);

		// Then
		verify(heroRankingRepository, times(1)).save(ranking);
	}

}
