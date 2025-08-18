package com.abe.gg_stats.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.abe.gg_stats.entity.ApiRateLimit;
import com.abe.gg_stats.repository.ApiRateLimitRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class OpenDotaApiServiceTest {

	@Mock
	private RestTemplate restTemplate;

	@Mock
	private ApiRateLimitRepository rateLimitRepository;

	@Mock
	private ObjectMapper objectMapper;

	@Mock
	private JsonNode jsonNode;

	private OpenDotaApiService apiService;

	@BeforeEach
	void setUp() {
		apiService = new OpenDotaApiService(restTemplate, rateLimitRepository, objectMapper);

		// Set default values using reflection
		ReflectionTestUtils.setField(apiService, "baseUrl", "https://api.opendota.com/api");
		ReflectionTestUtils.setField(apiService, "requestsPerMinute", 50);
		ReflectionTestUtils.setField(apiService, "requestsPerDay", 1800);
		ReflectionTestUtils.setField(apiService, "windowSizeMinutes", 1);
		ReflectionTestUtils.setField(apiService, "circuitBreakerThreshold", 5);
		ReflectionTestUtils.setField(apiService, "circuitBreakerTimeout", 30000L);
	}

	@Test
	void testMakeApiCall_Success_ShouldReturnJsonNode() throws Exception {
		// Given
		String endpoint = "/heroes";
		String responseBody = "{\"heroes\": []}";
		ResponseEntity<String> response = new ResponseEntity<>(responseBody, HttpStatus.OK);

		when(restTemplate.getForEntity(anyString(), eq(String.class))).thenReturn(response);
		when(objectMapper.readTree(responseBody)).thenReturn(jsonNode);
		when(rateLimitRepository.findByEndpoint(endpoint)).thenReturn(Optional.empty());
		when(rateLimitRepository.getTotalDailyRequests(any(LocalDate.class))).thenReturn(0);
		when(rateLimitRepository.save(any(ApiRateLimit.class))).thenAnswer(invocation -> {
			ApiRateLimit rateLimit = invocation.getArgument(0);
			if (rateLimit.getId() == null) {
				rateLimit.setId(1L);
			}
			return rateLimit;
		});

		// When
		Optional<JsonNode> result = apiService.makeApiCall(endpoint);

		// Then
		assertTrue(result.isPresent());
		assertEquals(jsonNode, result.get());
		verify(restTemplate).getForEntity("https://api.opendota.com/api/heroes", String.class);
		verify(rateLimitRepository).save(any(ApiRateLimit.class));
	}

	@Test
	void testMakeApiCall_HttpError_ShouldReturnEmpty() {
		// Given
		String endpoint = "/heroes";
		HttpClientErrorException httpError = new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Bad Request");

		when(restTemplate.getForEntity(anyString(), eq(String.class))).thenThrow(httpError);
		when(rateLimitRepository.findByEndpoint(endpoint)).thenReturn(Optional.empty());
		when(rateLimitRepository.getTotalDailyRequests(any(LocalDate.class))).thenReturn(0);
		when(rateLimitRepository.save(any(ApiRateLimit.class))).thenAnswer(invocation -> {
			ApiRateLimit rateLimit = invocation.getArgument(0);
			if (rateLimit.getId() == null) {
				rateLimit.setId(1L);
			}
			return rateLimit;
		});

		// When
		Optional<JsonNode> result = apiService.makeApiCall(endpoint);

		// Then
		assertFalse(result.isPresent());
		verify(restTemplate).getForEntity("https://api.opendota.com/api/heroes", String.class);
		verify(rateLimitRepository).save(any(ApiRateLimit.class)); // Rate limit is
																	// created during
																	// canMakeRequest
																	// check
	}

	@Test
	void testMakeApiCall_TooManyRequests_ShouldUpdateRateLimit() {
		// Given
		String endpoint = "/heroes";
		HttpClientErrorException rateLimitError = new HttpClientErrorException(HttpStatus.TOO_MANY_REQUESTS,
				"Too Many Requests");

		when(restTemplate.getForEntity(anyString(), eq(String.class))).thenThrow(rateLimitError);
		when(rateLimitRepository.findByEndpoint(endpoint)).thenReturn(Optional.empty());
		when(rateLimitRepository.getTotalDailyRequests(any(LocalDate.class))).thenReturn(0);
		when(rateLimitRepository.save(any(ApiRateLimit.class))).thenAnswer(invocation -> {
			ApiRateLimit rateLimit = invocation.getArgument(0);
			if (rateLimit.getId() == null) {
				rateLimit.setId(1L);
			}
			return rateLimit;
		});

		// When
		Optional<JsonNode> result = apiService.makeApiCall(endpoint);

		// Then
		assertFalse(result.isPresent());
		verify(restTemplate).getForEntity("https://api.opendota.com/api/heroes", String.class);
		verify(rateLimitRepository).save(any(ApiRateLimit.class));
	}

	@Test
	void testMakeApiCall_ConnectionError_ShouldReturnEmpty() {
		// Given
		String endpoint = "/heroes";
		ResourceAccessException connectionError = new ResourceAccessException("Connection failed");

		when(restTemplate.getForEntity(anyString(), eq(String.class))).thenThrow(connectionError);
		when(rateLimitRepository.findByEndpoint(endpoint)).thenReturn(Optional.empty());
		when(rateLimitRepository.getTotalDailyRequests(any(LocalDate.class))).thenReturn(0);
		when(rateLimitRepository.save(any(ApiRateLimit.class))).thenAnswer(invocation -> {
			ApiRateLimit rateLimit = invocation.getArgument(0);
			if (rateLimit.getId() == null) {
				rateLimit.setId(1L);
			}
			return rateLimit;
		});

		// When
		Optional<JsonNode> result = apiService.makeApiCall(endpoint);

		// Then
		assertFalse(result.isPresent());
		verify(restTemplate).getForEntity("https://api.opendota.com/api/heroes", String.class);
		verify(rateLimitRepository).save(any(ApiRateLimit.class)); // Rate limit is
																	// created during
																	// canMakeRequest
																	// check
	}

	@Test
	void testMakeApiCall_UnexpectedException_ShouldReturnEmpty() {
		// Given
		String endpoint = "/heroes";
		RuntimeException unexpectedError = new RuntimeException("Unexpected error");

		when(restTemplate.getForEntity(anyString(), eq(String.class))).thenThrow(unexpectedError);
		when(rateLimitRepository.findByEndpoint(endpoint)).thenReturn(Optional.empty());
		when(rateLimitRepository.getTotalDailyRequests(any(LocalDate.class))).thenReturn(0);
		when(rateLimitRepository.save(any(ApiRateLimit.class))).thenAnswer(invocation -> {
			ApiRateLimit rateLimit = invocation.getArgument(0);
			if (rateLimit.getId() == null) {
				rateLimit.setId(1L);
			}
			return rateLimit;
		});

		// When
		Optional<JsonNode> result = apiService.makeApiCall(endpoint);

		// Then
		assertFalse(result.isPresent());
		verify(restTemplate).getForEntity("https://api.opendota.com/api/heroes", String.class);
		verify(rateLimitRepository).save(any(ApiRateLimit.class)); // Rate limit is
																	// created during
																	// canMakeRequest
																	// check
	}

	@Test
	void testMakeApiCall_EmptyResponseBody_ShouldReturnEmpty() throws Exception {
		// Given
		String endpoint = "/heroes";
		ResponseEntity<String> response = new ResponseEntity<>(null, HttpStatus.OK);

		when(restTemplate.getForEntity(anyString(), eq(String.class))).thenReturn(response);
		when(rateLimitRepository.findByEndpoint(endpoint)).thenReturn(Optional.empty());
		when(rateLimitRepository.getTotalDailyRequests(any(LocalDate.class))).thenReturn(0);
		when(rateLimitRepository.save(any(ApiRateLimit.class))).thenAnswer(invocation -> {
			ApiRateLimit rateLimit = invocation.getArgument(0);
			if (rateLimit.getId() == null) {
				rateLimit.setId(1L);
			}
			return rateLimit;
		});

		// When
		Optional<JsonNode> result = apiService.makeApiCall(endpoint);

		// Then
		assertFalse(result.isPresent());
		verify(restTemplate).getForEntity("https://api.opendota.com/api/heroes", String.class);
		verify(rateLimitRepository).save(any(ApiRateLimit.class)); // Rate limit is
																	// created during
																	// canMakeRequest
																	// check
	}

	@Test
	void testMakeApiCall_NonOkStatus_ShouldReturnEmpty() {
		// Given
		String endpoint = "/heroes";
		ResponseEntity<String> response = new ResponseEntity<>("Error", HttpStatus.INTERNAL_SERVER_ERROR);

		when(restTemplate.getForEntity(anyString(), eq(String.class))).thenReturn(response);
		when(rateLimitRepository.findByEndpoint(endpoint)).thenReturn(Optional.empty());
		when(rateLimitRepository.getTotalDailyRequests(any(LocalDate.class))).thenReturn(0);
		when(rateLimitRepository.save(any(ApiRateLimit.class))).thenAnswer(invocation -> {
			ApiRateLimit rateLimit = invocation.getArgument(0);
			if (rateLimit.getId() == null) {
				rateLimit.setId(1L);
			}
			return rateLimit;
		});

		// When
		Optional<JsonNode> result = apiService.makeApiCall(endpoint);

		// Then
		assertFalse(result.isPresent());
		verify(restTemplate).getForEntity("https://api.opendota.com/api/heroes", String.class);
		verify(rateLimitRepository).save(any(ApiRateLimit.class)); // Rate limit is
																	// created during
																	// canMakeRequest
																	// check
	}

	@Test
	void testMakeApiCall_JsonParseError_ShouldReturnEmpty() throws Exception {
		// Given
		String endpoint = "/heroes";
		String responseBody = "{\"heroes\": []}";
		ResponseEntity<String> response = new ResponseEntity<>(responseBody, HttpStatus.OK);

		when(restTemplate.getForEntity(anyString(), eq(String.class))).thenReturn(response);
		when(objectMapper.readTree(responseBody)).thenThrow(new RuntimeException("JSON parse error"));
		when(rateLimitRepository.findByEndpoint(endpoint)).thenReturn(Optional.empty());
		when(rateLimitRepository.getTotalDailyRequests(any(LocalDate.class))).thenReturn(0);
		when(rateLimitRepository.save(any(ApiRateLimit.class))).thenAnswer(invocation -> {
			ApiRateLimit rateLimit = invocation.getArgument(0);
			if (rateLimit.getId() == null) {
				rateLimit.setId(1L);
			}
			return rateLimit;
		});

		// When
		Optional<JsonNode> result = apiService.makeApiCall(endpoint);

		// Then
		assertFalse(result.isPresent());
		verify(restTemplate).getForEntity("https://api.opendota.com/api/heroes", String.class);
		verify(rateLimitRepository).save(any(ApiRateLimit.class)); // Rate limit is
																	// created during
																	// canMakeRequest
																	// check
	}

	@Test
	void testGetRemainingDailyRequests_WithRequests_ShouldReturnCorrectValue() {
		// Given
		when(rateLimitRepository.getTotalDailyRequests(any())).thenReturn(500);

		// When
		int remaining = apiService.getRemainingDailyRequests();

		// Then
		assertEquals(1300, remaining); // 1800 - 500
		verify(rateLimitRepository).getTotalDailyRequests(any());
	}

	@Test
	void testGetRemainingDailyRequests_NoRequests_ShouldReturnMaxValue() {
		// Given
		when(rateLimitRepository.getTotalDailyRequests(any())).thenReturn(null);

		// When
		int remaining = apiService.getRemainingDailyRequests();

		// Then
		assertEquals(1800, remaining); // 1800 - 0
		verify(rateLimitRepository).getTotalDailyRequests(any());
	}

	@Test
	void testGetRemainingDailyRequests_MaxRequests_ShouldReturnZero() {
		// Given
		when(rateLimitRepository.getTotalDailyRequests(any())).thenReturn(1800);

		// When
		int remaining = apiService.getRemainingDailyRequests();

		// Then
		assertEquals(0, remaining); // 1800 - 1800
		verify(rateLimitRepository).getTotalDailyRequests(any());
	}

	@Test
	void testGetRemainingDailyRequests_ExceededRequests_ShouldReturnNegative() {
		// Given
		when(rateLimitRepository.getTotalDailyRequests(any())).thenReturn(2000);

		// When
		int remaining = apiService.getRemainingDailyRequests();

		// Then
		assertEquals(-200, remaining); // 1800 - 2000
		verify(rateLimitRepository).getTotalDailyRequests(any());
	}

	@Test
	void testGetRemainingMinuteRequests_WithValidRateLimit_ShouldReturnCorrectValue() {
		// Given
		String endpoint = "/heroes";
		ApiRateLimit rateLimit = ApiRateLimit.builder()
			.endpoint(endpoint)
			.requestsCount(30)
			.windowStart(LocalDateTime.now())
			.build();

		when(rateLimitRepository.findByEndpoint(endpoint)).thenReturn(Optional.of(rateLimit));

		// When
		int remaining = apiService.getRemainingMinuteRequests(endpoint);

		// Then
		assertEquals(20, remaining); // 50 - 30
		verify(rateLimitRepository).findByEndpoint(endpoint);
	}

	@Test
	void testGetRemainingMinuteRequests_NoRateLimit_ShouldReturnMaxValue() {
		// Given
		String endpoint = "/heroes";
		when(rateLimitRepository.findByEndpoint(endpoint)).thenReturn(Optional.empty());

		// When
		int remaining = apiService.getRemainingMinuteRequests(endpoint);

		// Then
		assertEquals(50, remaining); // requestsPerMinute
		verify(rateLimitRepository).findByEndpoint(endpoint);
	}

	@Test
	void testGetRateLimitStatus_WithExistingRateLimit_ShouldReturnRateLimit() {
		// Given
		String endpoint = "/heroes";
		ApiRateLimit rateLimit = ApiRateLimit.builder().endpoint(endpoint).requestsCount(10).build();

		when(rateLimitRepository.findByEndpoint(endpoint)).thenReturn(Optional.of(rateLimit));

		// When
		Optional<ApiRateLimit> result = apiService.getRateLimitStatus(endpoint);

		// Then
		assertTrue(result.isPresent());
		assertEquals(rateLimit, result.get());
		verify(rateLimitRepository).findByEndpoint(endpoint);
	}

	@Test
	void testGetRateLimitStatus_WithNoRateLimit_ShouldReturnEmpty() {
		// Given
		String endpoint = "/heroes";
		when(rateLimitRepository.findByEndpoint(endpoint)).thenReturn(Optional.empty());

		// When
		Optional<ApiRateLimit> result = apiService.getRateLimitStatus(endpoint);

		// Then
		assertFalse(result.isPresent());
		verify(rateLimitRepository).findByEndpoint(endpoint);
	}

	@Test
	void testGetHeroes_ShouldCallMakeApiCall() throws Exception {
		// Given
		when(restTemplate.getForEntity(anyString(), eq(String.class)))
			.thenReturn(new ResponseEntity<>("{\"heroes\": []}", HttpStatus.OK));
		when(objectMapper.readTree(anyString())).thenReturn(jsonNode);
		when(rateLimitRepository.findByEndpoint("/heroes")).thenReturn(Optional.empty());
		when(rateLimitRepository.getTotalDailyRequests(any(LocalDate.class))).thenReturn(0);
		when(rateLimitRepository.save(any(ApiRateLimit.class))).thenAnswer(invocation -> {
			ApiRateLimit rateLimit = invocation.getArgument(0);
			if (rateLimit.getId() == null) {
				rateLimit.setId(1L);
			}
			return rateLimit;
		});

		// When
		Optional<JsonNode> result = apiService.getHeroes();

		// Then
		assertTrue(result.isPresent());
		verify(restTemplate).getForEntity("https://api.opendota.com/api/heroes", String.class);
	}

	@Test
	void testGetProPlayers_ShouldCallMakeApiCall() throws Exception {
		// Given
		when(restTemplate.getForEntity(anyString(), eq(String.class)))
			.thenReturn(new ResponseEntity<>("{\"proPlayers\": []}", HttpStatus.OK));
		when(objectMapper.readTree(anyString())).thenReturn(jsonNode);
		when(rateLimitRepository.findByEndpoint("/proPlayers")).thenReturn(Optional.empty());
		when(rateLimitRepository.getTotalDailyRequests(any(LocalDate.class))).thenReturn(0);
		when(rateLimitRepository.save(any(ApiRateLimit.class))).thenAnswer(invocation -> {
			ApiRateLimit rateLimit = invocation.getArgument(0);
			if (rateLimit.getId() == null) {
				rateLimit.setId(1L);
			}
			return rateLimit;
		});

		// When
		Optional<JsonNode> result = apiService.getProPlayers();

		// Then
		assertTrue(result.isPresent());
		verify(restTemplate).getForEntity("https://api.opendota.com/api/proPlayers", String.class);
	}

	@Test
	void testGetTeams_ShouldCallMakeApiCall() throws Exception {
		// Given
		when(restTemplate.getForEntity(anyString(), eq(String.class)))
			.thenReturn(new ResponseEntity<>("{\"teams\": []}", HttpStatus.OK));
		when(objectMapper.readTree(anyString())).thenReturn(jsonNode);
		when(rateLimitRepository.findByEndpoint("/teams")).thenReturn(Optional.empty());
		when(rateLimitRepository.getTotalDailyRequests(any(LocalDate.class))).thenReturn(0);
		when(rateLimitRepository.save(any(ApiRateLimit.class))).thenAnswer(invocation -> {
			ApiRateLimit rateLimit = invocation.getArgument(0);
			if (rateLimit.getId() == null) {
				rateLimit.setId(1L);
			}
			return rateLimit;
		});

		// When
		Optional<JsonNode> result = apiService.getTeams();

		// Then
		assertTrue(result.isPresent());
		verify(restTemplate).getForEntity("https://api.opendota.com/api/teams", String.class);
	}

	@Test
	void testGetPlayer_ShouldCallMakeApiCall() throws Exception {
		// Given
		Long accountId = 12345L;
		when(restTemplate.getForEntity(anyString(), eq(String.class)))
			.thenReturn(new ResponseEntity<>("{\"player\": {}}", HttpStatus.OK));
		when(objectMapper.readTree(anyString())).thenReturn(jsonNode);
		when(rateLimitRepository.findByEndpoint("/players/12345")).thenReturn(Optional.empty());
		when(rateLimitRepository.getTotalDailyRequests(any(LocalDate.class))).thenReturn(0);
		when(rateLimitRepository.save(any(ApiRateLimit.class))).thenAnswer(invocation -> {
			ApiRateLimit rateLimit = invocation.getArgument(0);
			if (rateLimit.getId() == null) {
				rateLimit.setId(1L);
			}
			return rateLimit;
		});

		// When
		Optional<JsonNode> result = apiService.getPlayer(accountId);

		// Then
		assertTrue(result.isPresent());
		verify(restTemplate).getForEntity("https://api.opendota.com/api/players/12345", String.class);
	}

	@Test
	void testGetPlayerRanking_ShouldCallMakeApiCall() throws Exception {
		// Given
		Long accountId = 12345L;
		when(restTemplate.getForEntity(anyString(), eq(String.class)))
			.thenReturn(new ResponseEntity<>("{\"rankings\": []}", HttpStatus.OK));
		when(objectMapper.readTree(anyString())).thenReturn(jsonNode);
		when(rateLimitRepository.findByEndpoint("/players/12345/rankings")).thenReturn(Optional.empty());
		when(rateLimitRepository.getTotalDailyRequests(any(LocalDate.class))).thenReturn(0);
		when(rateLimitRepository.save(any(ApiRateLimit.class))).thenAnswer(invocation -> {
			ApiRateLimit rateLimit = invocation.getArgument(0);
			if (rateLimit.getId() == null) {
				rateLimit.setId(1L);
			}
			return rateLimit;
		});

		// When
		Optional<JsonNode> result = apiService.getPlayerRanking(accountId);

		// Then
		assertTrue(result.isPresent());
		verify(restTemplate).getForEntity("https://api.opendota.com/api/players/12345/rankings", String.class);
	}

	@Test
	void testGetHeroRanking_ShouldCallMakeApiCall() throws Exception {
		// Given
		Integer heroId = 1;
		when(restTemplate.getForEntity(anyString(), eq(String.class)))
			.thenReturn(new ResponseEntity<>("{\"rankings\": []}", HttpStatus.OK));
		when(objectMapper.readTree(anyString())).thenReturn(jsonNode);
		when(rateLimitRepository.findByEndpoint("/rankings?hero_id=1")).thenReturn(Optional.empty());
		when(rateLimitRepository.getTotalDailyRequests(any(LocalDate.class))).thenReturn(0);
		when(rateLimitRepository.save(any(ApiRateLimit.class))).thenAnswer(invocation -> {
			ApiRateLimit rateLimit = invocation.getArgument(0);
			if (rateLimit.getId() == null) {
				rateLimit.setId(1L);
			}
			return rateLimit;
		});

		// When
		Optional<JsonNode> result = apiService.getHeroRanking(heroId);

		// Then
		assertTrue(result.isPresent());
		verify(restTemplate).getForEntity("https://api.opendota.com/api/rankings?hero_id=1", String.class);
	}

	@Test
	void testGetCircuitBreakerStatus_ShouldReturnStatusString() {
		// When
		String status = apiService.getCircuitBreakerStatus();

		// Then
		assertNotNull(status);
		assertTrue(status.contains("Circuit Breaker Status"));
		assertTrue(status.contains("Open:"));
		assertTrue(status.contains("Failures:"));
		assertTrue(status.contains("Last Failure:"));
	}

}
