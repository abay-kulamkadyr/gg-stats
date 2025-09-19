package com.abe.gg_stats.batch;

import com.abe.gg_stats.batch.hero_ranking.HeroRankingReader;
import com.abe.gg_stats.repository.HeroRepository;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HeroRankingReaderTest {

	@Mock
	private HeroRepository heroRepository;

	private HeroRankingReader reader;

	@BeforeEach
	void setUp() {
		reader = new HeroRankingReader(heroRepository);
	}

	@Test
	void read_ShouldReturnIdsSequentially_thenNull() throws Exception {
		when(heroRepository.findAllIds()).thenReturn(Arrays.asList(1, 2));

		Integer first = reader.read();
		Integer second = reader.read();
		Integer third = reader.read();

		assertEquals(1, first);
		assertEquals(2, second);
		assertNull(third);
		verify(heroRepository).findAllIds();
	}

	@Test
	void read_WithEmptyList_ShouldReturnNull() throws Exception {
		when(heroRepository.findAllIds()).thenReturn(List.of());
		Integer result = reader.read();
		assertNull(result);
		verify(heroRepository).findAllIds();
	}

}
