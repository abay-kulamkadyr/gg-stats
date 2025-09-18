package com.abe.gg_stats.batch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.abe.gg_stats.batch.hero.HeroesReader;
import com.abe.gg_stats.config.batch.BatchExpirationConfig;
import com.abe.gg_stats.repository.HeroRepository;
import com.abe.gg_stats.service.OpenDotaApiService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class HeroReaderTest {

	@Mock
	private OpenDotaApiService openDotaApiService;

	@Mock
	private HeroRepository heroRepository;

	@Mock
	private BatchExpirationConfig expirationConfig;

	@InjectMocks
	private HeroesReader heroesReader;

	private ObjectMapper objectMapper;

	@BeforeEach
	void setup() {
		objectMapper = new ObjectMapper();
	}

	@Test
	void read_WhenCacheFresh_ShouldNotCallApiAndReturnNull() {
		when(expirationConfig.getDurationByConfigName(anyString())).thenReturn(Duration.ofDays(30));
		when(heroRepository.findMaxUpdatedAt()).thenReturn(Optional.of(Instant.now().minus(Duration.ofHours(1))));

		JsonNode result = heroesReader.read();

		assertNull(result);
		verify(openDotaApiService, never()).getHeroes();
	}

	@Test
	void read_WhenCacheExpired_ShouldFetchFromApiAndReturnItems() throws Exception {
		when(expirationConfig.getDurationByConfigName(anyString())).thenReturn(Duration.ofDays(30));
		when(heroRepository.findMaxUpdatedAt()).thenReturn(Optional.of(Instant.now().minus(Duration.ofDays(40))));

		String json = """
				    [
				      {"id":1,"name":"antimage","localized_name":"Anti-Mage"},
				      {"id":2,"name":"axe","localized_name":"Axe"}
				    ]
				""";
		JsonNode array = objectMapper.readTree(json);
		when(openDotaApiService.getHeroes()).thenReturn(Optional.of(array));

		JsonNode first = heroesReader.read();
		JsonNode second = heroesReader.read();
		JsonNode third = heroesReader.read();

		assertNotNull(first);
		assertEquals(1, first.get("id").asInt());
		assertNotNull(second);
		assertEquals(2, second.get("id").asInt());
		assertNull(third);

		verify(openDotaApiService, times(1)).getHeroes();
	}

	@Test
	void read_WhenNoExistingData_ShouldFetchFromApi() throws Exception {
		when(heroRepository.findMaxUpdatedAt()).thenReturn(Optional.empty());

		String json = """
				    [
				      {"id":10,"name":"bane","localized_name":"Bane"}
				    ]
				""";
		JsonNode array = objectMapper.readTree(json);
		when(openDotaApiService.getHeroes()).thenReturn(Optional.of(array));

		JsonNode first = heroesReader.read();
		JsonNode second = heroesReader.read();

		assertNotNull(first);
		assertEquals(10, first.get("id").asInt());
		assertNull(second);
		verify(openDotaApiService, times(1)).getHeroes();
	}

}
