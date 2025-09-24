package com.abe.gg_stats.service;

import com.abe.gg_stats.dto.response.HeroPairsDto;
import com.abe.gg_stats.dto.response.HighlightsDto;
import com.abe.gg_stats.dto.response.HighlightsHeroDto;
import com.abe.gg_stats.exception.HighlightsNotFoundException;
import com.abe.gg_stats.repository.jdbc.HighlightsDao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class HighlightsServiceTest {

	private HighlightsDao dao;

	private HighlightsService service;

	@BeforeEach
	void setUp() {
		dao = mock(HighlightsDao.class);
		service = new HighlightsService(dao);
	}

	@Test
	void throwsWhenNoBucketValueAvailable() {
		// value is null and dao returns null for latest and offset
		when(dao.latestBucketValue("patch")).thenReturn(null);
		when(dao.bucketValueByOffset("patch", 1)).thenReturn(null);

		assertThrows(HighlightsNotFoundException.class, () -> {
			service.getHighlights("patch", null, 5, "lift", 1);
		});

		verify(dao).bucketValueByOffset("patch", 1);
		verifyNoMoreInteractions(dao);
	}

	@Test
	void returnsHighlightsWhenValueProvided() {
		String bucket = "patch";
		String value = "7.36";
		int limit = 5;
		String sort = "lift";
		int weekOffset = 0;

		when(dao.matchesForBucket(bucket, value)).thenReturn(100L);
		when(dao.topHeroes(bucket, value, limit)).thenReturn(List.of(new HighlightsHeroDto(1, 100, 60, 0.6, 0.05)));
		when(dao.topPairs(bucket, value, limit, sort))
			.thenReturn(List.of(new HeroPairsDto(1, 2, 40, 0.8, 0.9, 1.2, 0.1, 0.2)));

		HighlightsDto dto = service.getHighlights(bucket, value, limit, sort, weekOffset);
		assertNotNull(dto);
		assertEquals(100L, dto.matches());
		assertEquals(1, dto.heroes().size());
		assertEquals(1, dto.pairs().size());

		verify(dao).matchesForBucket(bucket, value);
		verify(dao).topHeroes(bucket, value, limit);
		verify(dao).topPairs(bucket, value, limit, sort);
		verifyNoMoreInteractions(dao);
	}

	@Test
	void getPairHighlightsFallsBackToSortMapping() {
		when(dao.latestBucketValue("patch_week")).thenReturn("2025-W37");
		when(dao.matchesForBucket("patch_week", "2025-W37")).thenReturn(10L);
		when(dao.topPairsWithHeroes(eq("patch_week"), eq("2025-W37"), anyInt(), eq("delta_lift")))
			.thenReturn(List.of());

		assertNotNull(service.getPairHighlights("emerging", 0, 10));

		verify(dao).topPairsWithHeroes("patch_week", "2025-W37", 10, "delta_lift");
	}

}
