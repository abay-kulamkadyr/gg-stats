package com.abe.gg_stats.service;

import com.abe.gg_stats.entity.ApiRateLimit;
import com.abe.gg_stats.repository.ApiRateLimitRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SimpleOpenDotaApiServiceTest {

	@Mock
	private ApiRateLimitRepository rateLimitRepository;

	private OpenDotaApiService apiService;

	@BeforeEach
	void setUp() {
		apiService = new OpenDotaApiService(null, rateLimitRepository, null);
		ReflectionTestUtils.setField(apiService, "requestsPerMinute", 50);
		ReflectionTestUtils.setField(apiService, "requestsPerDay", 1800);
		ReflectionTestUtils.setField(apiService, "windowSizeMinutes", 1);
	}

	@Test
	void testGetRemainingDailyRequests_ShouldReturnCorrectValue() {
		// Given
		when(rateLimitRepository.getTotalDailyRequests(any())).thenReturn(500);

		// When
		int remaining = apiService.getRemainingDailyRequests();

		// Then
		assertEquals(1300, remaining); // 1800 - 500
	}

	@Test
	void testGetRemainingDailyRequests_NoRequests_ShouldReturnMaxValue() {
		// Given
		when(rateLimitRepository.getTotalDailyRequests(any())).thenReturn(null);

		// When
		int remaining = apiService.getRemainingDailyRequests();

		// Then
		assertEquals(1800, remaining); // 1800 - 0
	}

}
