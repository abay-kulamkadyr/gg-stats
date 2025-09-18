package com.abe.gg_stats.batch;

import com.abe.gg_stats.batch.hero_ranking.HeroRankingWriter;
import com.abe.gg_stats.dto.request.opendota.OpenDotaHeroRankingDto;
import com.abe.gg_stats.dto.request.opendota.mapper.OpenDotaHeroRankingMapper;
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

	@Mock
	private OpenDotaHeroRankingMapper openDotaHeroRankingMapper;

	private HeroRankingWriter writer;

	@BeforeEach
	void setUp() {
		writer = new HeroRankingWriter(heroRankingRepository, openDotaHeroRankingMapper);
	}

	@Test
	void testWrite_ValidChunk_ShouldSaveAll() throws Exception {
		List<OpenDotaHeroRankingDto> rankings1 = Arrays.asList(new OpenDotaHeroRankingDto(12345L, 1, 95.5),
				new OpenDotaHeroRankingDto(67890L, 1, 88.0));
		List<HeroRanking> rankings1Entities = Arrays.asList(createHeroRanking(1, 12345L, 95.5),
				createHeroRanking(1, 67890L, 88.0));

		List<OpenDotaHeroRankingDto> rankings2 = Arrays.asList(new OpenDotaHeroRankingDto(11111L, 2, 92.0));
		List<HeroRanking> rankings2Entities = Arrays.asList(createHeroRanking(2, 11111L, 92.0));

		when(openDotaHeroRankingMapper.dtoToEntity(rankings1)).thenReturn(rankings1Entities);
		when(openDotaHeroRankingMapper.dtoToEntity(rankings2)).thenReturn(rankings2Entities);

		Chunk<List<OpenDotaHeroRankingDto>> chunk = new Chunk<>(Arrays.asList(rankings1, rankings2));

		writer.write(chunk);

		verify(heroRankingRepository, times(2)).saveAll(any(List.class));
		verify(heroRankingRepository).saveAll(rankings1Entities);
		verify(heroRankingRepository).saveAll(rankings2Entities);
	}

	@Test
	void testWrite_EmptyChunk_ShouldHandleGracefully() throws Exception {
		Chunk<List<OpenDotaHeroRankingDto>> chunk = new Chunk<>(Collections.emptyList());
		writer.write(chunk);
		verify(heroRankingRepository, never()).saveAll(any(List.class));
	}

	@Test
	void testWrite_NullChunk_ShouldHandleGracefully() {
		assertThrows(NullPointerException.class, () -> writer.write(null));
		verify(heroRankingRepository, never()).saveAll(any(List.class));
	}

	@Test
	void testWrite_ChunkWithNullItems_ShouldHandleGracefully() throws Exception {
		List<OpenDotaHeroRankingDto> rankings1 = Arrays.asList(new OpenDotaHeroRankingDto(12345L, 1, 95.5),
				new OpenDotaHeroRankingDto(67890L, 1, 88.0));
		List<HeroRanking> rankings1Entities = Arrays.asList(createHeroRanking(1, 12345L, 95.5),
				createHeroRanking(1, 67890L, 88.0));
		when(openDotaHeroRankingMapper.dtoToEntity(rankings1)).thenReturn(rankings1Entities);
		Chunk<List<OpenDotaHeroRankingDto>> chunk = new Chunk<>(Arrays.asList(rankings1, null));
		writer.write(chunk);
		verify(heroRankingRepository, times(1)).saveAll(rankings1Entities);
	}

	@Test
	void testWrite_SingleItem_ShouldSaveSuccessfully() throws Exception {
		List<OpenDotaHeroRankingDto> rankings = Arrays.asList(new OpenDotaHeroRankingDto(12345L, 1, 95.5));
		List<HeroRanking> entities = Arrays.asList(createHeroRanking(1, 12345L, 95.5));
		when(openDotaHeroRankingMapper.dtoToEntity(rankings)).thenReturn(entities);
		Chunk<List<OpenDotaHeroRankingDto>> chunk = new Chunk<>(Collections.singletonList(rankings));
		writer.write(chunk);
		verify(heroRankingRepository, times(1)).saveAll(entities);
	}

	@Test
	void testWrite_DatabaseError_ShouldContinueProcessing() throws Exception {
		List<OpenDotaHeroRankingDto> rankings1 = Arrays.asList(new OpenDotaHeroRankingDto(12345L, 1, 95.5));
		List<HeroRanking> entities1 = Arrays.asList(createHeroRanking(1, 12345L, 95.5));
		List<OpenDotaHeroRankingDto> rankings2 = Arrays.asList(new OpenDotaHeroRankingDto(67890L, 2, 88.0));
		List<HeroRanking> entities2 = Arrays.asList(createHeroRanking(2, 67890L, 88.0));
		when(openDotaHeroRankingMapper.dtoToEntity(rankings1)).thenReturn(entities1);
		when(openDotaHeroRankingMapper.dtoToEntity(rankings2)).thenReturn(entities2);
		Chunk<List<OpenDotaHeroRankingDto>> chunk = new Chunk<>(Arrays.asList(rankings1, rankings2));
		when(heroRankingRepository.saveAll(entities1)).thenReturn(entities1);
		when(heroRankingRepository.saveAll(entities2)).thenThrow(new RuntimeException("Database error"));
		writer.write(chunk);
		verify(heroRankingRepository).saveAll(entities1);
		verify(heroRankingRepository).saveAll(entities2);
	}

	@Test
	void testWrite_LargeChunk_ShouldHandleCorrectly() throws Exception {
		Chunk<List<OpenDotaHeroRankingDto>> chunk = new Chunk<>();
		for (int i = 0; i < 100; i++) {
			List<OpenDotaHeroRankingDto> rankings = Arrays.asList(new OpenDotaHeroRankingDto((long) i, i, 50.0 + i));
			chunk.add(rankings);
		}
		// Mock mapper to return non-null for all calls
		when(openDotaHeroRankingMapper.dtoToEntity(any(Iterable.class)))
			.thenAnswer(invocation -> Collections.emptyList());
		writer.write(chunk);
		verify(heroRankingRepository, times(100)).saveAll(any(List.class));
	}

	@Test
	void testWrite_ChunkWithMixedData_ShouldHandleCorrectly() throws Exception {
		List<OpenDotaHeroRankingDto> rankings1 = Arrays.asList(new OpenDotaHeroRankingDto(12345L, 1, 95.5));
		List<OpenDotaHeroRankingDto> rankings2 = Arrays.asList(new OpenDotaHeroRankingDto(67890L, 2, null));
		List<OpenDotaHeroRankingDto> rankings3 = Arrays.asList(new OpenDotaHeroRankingDto(11111L, 3, 100.0));
		when(openDotaHeroRankingMapper.dtoToEntity(rankings1)).thenReturn(Collections.emptyList());
		when(openDotaHeroRankingMapper.dtoToEntity(rankings2)).thenReturn(Collections.emptyList());
		when(openDotaHeroRankingMapper.dtoToEntity(rankings3)).thenReturn(Collections.emptyList());
		Chunk<List<OpenDotaHeroRankingDto>> chunk = new Chunk<>(Arrays.asList(rankings1, rankings2, rankings3));
		writer.write(chunk);
		verify(heroRankingRepository, times(3)).saveAll(any(List.class));
	}

	@Test
	void testWrite_ChunkWithZeroValues_ShouldHandleCorrectly() throws Exception {
		List<OpenDotaHeroRankingDto> rankings = Arrays.asList(new OpenDotaHeroRankingDto(0L, 0, 0.0));
		when(openDotaHeroRankingMapper.dtoToEntity(rankings)).thenReturn(Collections.emptyList());
		Chunk<List<OpenDotaHeroRankingDto>> chunk = new Chunk<>(Collections.singletonList(rankings));
		writer.write(chunk);
		verify(heroRankingRepository, times(1)).saveAll(any(List.class));
	}

	@Test
	void testWrite_ChunkWithMaxValues_ShouldHandleCorrectly() throws Exception {
		List<OpenDotaHeroRankingDto> rankings = List
			.of(new OpenDotaHeroRankingDto(Long.MAX_VALUE, Integer.MAX_VALUE, Double.MAX_VALUE));
		when(openDotaHeroRankingMapper.dtoToEntity(rankings)).thenReturn(Collections.emptyList());
		Chunk<List<OpenDotaHeroRankingDto>> chunk = new Chunk<>(Collections.singletonList(rankings));
		writer.write(chunk);
		verify(heroRankingRepository, times(1)).saveAll(any(List.class));
	}

	@Test
	void testWrite_ChunkWithNegativeValues_ShouldHandleCorrectly() throws Exception {
		List<OpenDotaHeroRankingDto> rankings = Arrays.asList(new OpenDotaHeroRankingDto(-12345L, -1, -50.0));
		when(openDotaHeroRankingMapper.dtoToEntity(rankings)).thenReturn(Collections.emptyList());
		Chunk<List<OpenDotaHeroRankingDto>> chunk = new Chunk<>(Collections.singletonList(rankings));
		writer.write(chunk);
		verify(heroRankingRepository, times(1)).saveAll(any(List.class));
	}

	@Test
	void testWrite_EmptyRankingsList_ShouldHandleCorrectly() throws Exception {
		List<OpenDotaHeroRankingDto> emptyRankings = Collections.emptyList();
		Chunk<List<OpenDotaHeroRankingDto>> chunk = new Chunk<>(Collections.singletonList(emptyRankings));
		writer.write(chunk);
		verify(heroRankingRepository, never()).saveAll(any(List.class));
	}

	@Test
	void testWrite_MultipleRankingsInSingleList_ShouldSaveAll() throws Exception {
		List<OpenDotaHeroRankingDto> multipleRankings = Arrays.asList(new OpenDotaHeroRankingDto(12345L, 1, 95.5),
				new OpenDotaHeroRankingDto(67890L, 1, 88.0), new OpenDotaHeroRankingDto(11111L, 1, 92.0));
		List<HeroRanking> entities = Arrays.asList(createHeroRanking(1, 12345L, 95.5),
				createHeroRanking(1, 67890L, 88.0), createHeroRanking(1, 11111L, 92.0));
		when(openDotaHeroRankingMapper.dtoToEntity(multipleRankings)).thenReturn(entities);
		Chunk<List<OpenDotaHeroRankingDto>> chunk = new Chunk<>(Collections.singletonList(multipleRankings));
		writer.write(chunk);
		verify(heroRankingRepository, times(1)).saveAll(entities);
	}

	private HeroRanking createHeroRanking(Integer heroId, Long accountId, Double score) {
		HeroRanking ranking = new HeroRanking();
		ranking.setHeroId(heroId);
		ranking.setAccountId(accountId);
		ranking.setScore(score);
		return ranking;
	}

}
