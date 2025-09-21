package com.abe.gg_stats.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import com.abe.gg_stats.service.circuit_breaker.CircuitBreakerService;
import com.abe.gg_stats.service.rate_limit.OpenDotaRateLimitingService;

import java.util.function.Supplier;

class OpenDotaApiServiceTest {

	private RestTemplate restTemplate;

	private OpenDotaRateLimitingService rateLimit;

	private CircuitBreakerService circuitBreaker;

	private ObjectMapper objectMapper;

	private SimpleMeterRegistry meterRegistry;

	private OpenDotaApiService service;

	@BeforeEach
	void setUp() {
		restTemplate = mock(RestTemplate.class);
		rateLimit = mock(OpenDotaRateLimitingService.class);
		circuitBreaker = mock(CircuitBreakerService.class);
		objectMapper = new ObjectMapper();
		meterRegistry = new SimpleMeterRegistry();

		service = new OpenDotaApiService(restTemplate, rateLimit, circuitBreaker, objectMapper, meterRegistry);
		// Avoid NPE from @Value defaults by reflecting in baseUrl
		org.springframework.test.util.ReflectionTestUtils.setField(service, "baseUrl", "https://api.opendota.com/api");
		org.springframework.test.util.ReflectionTestUtils.setField(service, "healthCheckEnabled", true);
	}

	@Test
	void getHeroesHappyPathReturnsData() {
		when(rateLimit.tryAcquirePermit(anyString())).thenReturn(OpenDotaRateLimitingService.RateLimitResult.success());
		when(circuitBreaker.executeWithCircuitBreaker(eq("opendota_api"), any(), any())).thenAnswer(inv -> {
			@SuppressWarnings("unchecked")
			Supplier<Optional<?>> supplier = (Supplier<Optional<?>>) inv.getArgument(1);
			return supplier.get();
		});
		when(restTemplate.getForEntity(contains("/heroes"), eq(String.class)))
			.thenReturn(ResponseEntity.ok("[{\"id\":1}]"));

		Optional<?> res = service.getHeroes();
		assertTrue(res.isPresent());
	}

	@Test
	void returnsEmptyOnTooManyRequests() {
		when(rateLimit.tryAcquirePermit(anyString())).thenReturn(OpenDotaRateLimitingService.RateLimitResult.success());
		when(circuitBreaker.executeWithCircuitBreaker(eq("opendota_api"), any(), any())).thenAnswer(inv -> {
			@SuppressWarnings("unchecked")
			Supplier<Optional<?>> supplier = (Supplier<Optional<?>>) inv.getArgument(1);
			return supplier.get();
		});
		when(restTemplate.getForEntity(contains("/heroes"), eq(String.class)))
			.thenThrow(HttpClientErrorException.create(HttpStatus.TOO_MANY_REQUESTS, "", null, null, null));

		Optional<?> res = service.getHeroes();
		assertTrue(res.isEmpty());
	}

	@Test
	void getPlayerInvalidIdReturnsEmpty() {
		assertTrue(service.getPlayer(0L).isEmpty());
		assertTrue(service.getHeroRanking(0).isEmpty());
	}

}
