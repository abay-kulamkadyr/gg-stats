package com.abe.gg_stats.batch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.abe.gg_stats.batch.hero_ranking.HeroRankingProcessor;
import com.abe.gg_stats.config.batch.BatchExpirationConfig;
import com.abe.gg_stats.dto.request.opendota.OpenDotaHeroRankingDto;
import com.abe.gg_stats.repository.HeroRankingRepository;
import com.abe.gg_stats.service.OpenDotaApiService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class HeroRankingProcessorTest {

	@Mock
	private OpenDotaApiService apiService;

	@Mock
	private HeroRankingRepository heroRankingRepository;

	@Mock
	private BatchExpirationConfig expirationConfig;

	@InjectMocks
	private HeroRankingProcessor processor;

	private final ObjectMapper mapper = new ObjectMapper();

	@Test
	void testProcess_RefreshNeeded_MapsListFromApi() throws Exception {
		int heroId = 1;
		when(heroRankingRepository.findLastUpdateByHeroId(heroId)).thenReturn(Optional.empty());

		String json = """
				{"hero_id":1,"rankings":[{"account_id":12345,"score":95.5},{"account_id":67890,"score":88.0}]}
				""";
		JsonNode apiResponse = mapper.readTree(json);
		when(apiService.getHeroRanking(heroId)).thenReturn(Optional.of(apiResponse));

		List<OpenDotaHeroRankingDto> result = processor.process(heroId);

		assertNotNull(result);
		assertEquals(2, result.size());
		assertEquals(1, result.get(0).heroId());
		assertEquals(12345L, result.get(0).accountId());
		assertEquals(95.5, result.get(0).score());
		assertEquals(67890L, result.get(1).accountId());
	}

	@Test
	void testProcess_NotExpired_ReturnsNull() throws Exception {
		int heroId = 2;
		when(heroRankingRepository.findLastUpdateByHeroId(heroId))
			.thenReturn(Optional.of(Instant.now().minus(Duration.ofDays(5))));

		List<OpenDotaHeroRankingDto> result = processor.process(heroId);
		assertNull(result);
	}

	@Test
	void testProcess_ApiEmpty_ReturnsNull() throws Exception {
		int heroId = 3;
		when(heroRankingRepository.findLastUpdateByHeroId(heroId)).thenReturn(Optional.empty());
		when(apiService.getHeroRanking(heroId)).thenReturn(Optional.empty());

		List<OpenDotaHeroRankingDto> result = processor.process(heroId);
		assertNull(result);
	}

	@Test
	void testProcess_EmptyRankingsArray_ReturnsEmptyList() throws Exception {
		int heroId = 4;
		when(heroRankingRepository.findLastUpdateByHeroId(heroId)).thenReturn(Optional.empty());
		String json = """
				{"hero_id":4,"rankings":[]}
				""";
		when(apiService.getHeroRanking(heroId)).thenReturn(Optional.of(mapper.readTree(json)));

		List<OpenDotaHeroRankingDto> result = processor.process(heroId);
		assertNotNull(result);
		assertTrue(result.isEmpty());
	}

}
