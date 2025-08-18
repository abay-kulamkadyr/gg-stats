package com.abe.gg_stats.controller;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.abe.gg_stats.entity.ApiRateLimit;
import com.abe.gg_stats.service.BatchSchedulerService;
import com.abe.gg_stats.service.OpenDotaApiService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class BatchControllerTest {

	@Mock
	private BatchSchedulerService batchSchedulerService;

	@Mock
	private OpenDotaApiService openDotaApiService;

	private BatchController controller;

	@BeforeEach
	void setUp() {
		controller = new BatchController(batchSchedulerService, openDotaApiService);
	}

	@Test
	void testGetSystemStatus_AllServicesHealthy_ShouldReturnHealthyStatus() {
		// Given
		when(batchSchedulerService.getSchedulerStatus()).thenReturn("Running");
		when(openDotaApiService.getRemainingDailyRequests()).thenReturn(1500);
		when(openDotaApiService.getCircuitBreakerStatus())
			.thenReturn("Circuit Breaker Status - Open: false, Failures: 0, Last Failure: 0ms ago");

		// When
		ResponseEntity<Map<String, Object>> response = controller.getSystemStatus();

		// Then
		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertNotNull(response.getBody());

		Map<String, Object> status = response.getBody();
		assertEquals("healthy", status.get("status"));
		assertEquals("Running", status.get("scheduler"));
		assertEquals(1500, ((Map<?, ?>) status.get("apiRateLimit")).get("remainingDailyRequests"));
		assertEquals(1800, ((Map<?, ?>) status.get("apiRateLimit")).get("totalDailyLimit"));
		assertNotNull(status.get("circuitBreaker"));

		verify(batchSchedulerService).getSchedulerStatus();
		verify(openDotaApiService).getRemainingDailyRequests();
		verify(openDotaApiService).getCircuitBreakerStatus();
	}

	@Test
	void testGetSystemStatus_ServiceException_ShouldReturnErrorStatus() {
		// Given
		when(batchSchedulerService.getSchedulerStatus()).thenThrow(new RuntimeException("Service error"));

		// When
		ResponseEntity<Map<String, Object>> response = controller.getSystemStatus();

		// Then
		assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
		assertNotNull(response.getBody());

		Map<String, Object> status = response.getBody();
		assertEquals("error", status.get("status"));
		assertNotNull(status.get("message"));
	}

	@Test
	void testTriggerHeroesUpdate_Success_ShouldReturnSuccessResponse() {
		// Given
		when(batchSchedulerService.triggerHeroesUpdate()).thenReturn(true);

		// When
		ResponseEntity<Map<String, Object>> response = controller.triggerHeroesUpdate();

		// Then
		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertNotNull(response.getBody());

		Map<String, Object> result = response.getBody();
		assertEquals("success", result.get("status"));
		assertEquals("Heroes update job triggered successfully", result.get("message"));

		verify(batchSchedulerService).triggerHeroesUpdate();
	}

	@Test
	void testTriggerHeroesUpdate_Failure_ShouldReturnBadRequest() {
		// Given
		when(batchSchedulerService.triggerHeroesUpdate()).thenReturn(false);

		// When
		ResponseEntity<Map<String, Object>> response = controller.triggerHeroesUpdate();

		// Then
		assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
		assertNotNull(response.getBody());

		Map<String, Object> result = response.getBody();
		assertEquals("failed", result.get("status"));
		assertEquals("Failed to trigger heroes update job - insufficient API requests or other constraints",
				result.get("message"));

		verify(batchSchedulerService).triggerHeroesUpdate();
	}

	@Test
	void testTriggerHeroesUpdate_Exception_ShouldReturnErrorResponse() {
		// Given
		when(batchSchedulerService.triggerHeroesUpdate()).thenThrow(new RuntimeException("Service error"));

		// When
		ResponseEntity<Map<String, Object>> response = controller.triggerHeroesUpdate();

		// Then
		assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
		assertNotNull(response.getBody());

		Map<String, Object> result = response.getBody();
		assertEquals("error", result.get("status"));
		assertNotNull(result.get("message"));
	}

	@Test
	void testTriggerPlayersUpdate_Success_ShouldReturnSuccessResponse() {
		// Given
		when(batchSchedulerService.triggerPlayerUpdate()).thenReturn(true);

		// When
		ResponseEntity<Map<String, Object>> response = controller.triggerPlayersUpdate();

		// Then
		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertNotNull(response.getBody());

		Map<String, Object> result = response.getBody();
		assertEquals("success", result.get("status"));
		assertEquals("Players update job triggered successfully", result.get("message"));

		verify(batchSchedulerService).triggerPlayerUpdate();
	}

	@Test
	void testTriggerPlayersUpdate_Failure_ShouldReturnBadRequest() {
		// Given
		when(batchSchedulerService.triggerPlayerUpdate()).thenReturn(false);

		// When
		ResponseEntity<Map<String, Object>> response = controller.triggerPlayersUpdate();

		// Then
		assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
		assertNotNull(response.getBody());

		Map<String, Object> result = response.getBody();
		assertEquals("failed", result.get("status"));
		assertEquals("Failed to trigger players update job - insufficient API requests or other constraints",
				result.get("message"));

		verify(batchSchedulerService).triggerPlayerUpdate();
	}

	@Test
	void testTriggerTeamsUpdate_Success_ShouldReturnSuccessResponse() {
		// Given
		when(batchSchedulerService.triggerTeamsUpdate()).thenReturn(true);

		// When
		ResponseEntity<Map<String, Object>> response = controller.triggerTeamsUpdate();

		// Then
		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertNotNull(response.getBody());

		Map<String, Object> result = response.getBody();
		assertEquals("success", result.get("status"));
		assertEquals("Teams update job triggered successfully", result.get("message"));

		verify(batchSchedulerService).triggerTeamsUpdate();
	}

	@Test
	void testTriggerTeamsUpdate_Failure_ShouldReturnBadRequest() {
		// Given
		when(batchSchedulerService.triggerTeamsUpdate()).thenReturn(false);

		// When
		ResponseEntity<Map<String, Object>> response = controller.triggerTeamsUpdate();

		// Then
		assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
		assertNotNull(response.getBody());

		Map<String, Object> result = response.getBody();
		assertEquals("failed", result.get("status"));
		assertEquals("Failed to trigger teams update job - insufficient API requests or other constraints",
				result.get("message"));

		verify(batchSchedulerService).triggerTeamsUpdate();
	}

	@Test
	void testTriggerNotablePlayersUpdate_Success_ShouldReturnSuccessResponse() {
		// Given
		when(batchSchedulerService.triggerNotablePlayerUpdate()).thenReturn(true);

		// When
		ResponseEntity<Map<String, Object>> response = controller.triggerNotablePlayersUpdate();

		// Then
		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertNotNull(response.getBody());

		Map<String, Object> result = response.getBody();
		assertEquals("success", result.get("status"));
		assertEquals("Notable players update job triggered successfully", result.get("message"));

		verify(batchSchedulerService).triggerNotablePlayerUpdate();
	}

	@Test
	void testTriggerNotablePlayersUpdate_Failure_ShouldReturnBadRequest() {
		// Given
		when(batchSchedulerService.triggerNotablePlayerUpdate()).thenReturn(false);

		// When
		ResponseEntity<Map<String, Object>> response = controller.triggerNotablePlayersUpdate();

		// Then
		assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
		assertNotNull(response.getBody());

		Map<String, Object> result = response.getBody();
		assertEquals("failed", result.get("status"));
		assertEquals("Failed to trigger notable players update job - insufficient API requests or other constraints",
				result.get("message"));

		verify(batchSchedulerService).triggerNotablePlayerUpdate();
	}

	@Test
	void testTriggerHeroRankingsUpdate_Success_ShouldReturnSuccessResponse() {
		// Given
		when(batchSchedulerService.triggerHeroRankingUpdate()).thenReturn(true);

		// When
		ResponseEntity<Map<String, Object>> response = controller.triggerHeroRankingsUpdate();

		// Then
		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertNotNull(response.getBody());

		Map<String, Object> result = response.getBody();
		assertEquals("success", result.get("status"));
		assertEquals("Hero rankings update job triggered successfully", result.get("message"));

		verify(batchSchedulerService).triggerHeroRankingUpdate();
	}

	@Test
	void testTriggerHeroRankingsUpdate_Failure_ShouldReturnBadRequest() {
		// Given
		when(batchSchedulerService.triggerHeroRankingUpdate()).thenReturn(false);

		// When
		ResponseEntity<Map<String, Object>> response = controller.triggerHeroRankingsUpdate();

		// Then
		assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
		assertNotNull(response.getBody());

		Map<String, Object> result = response.getBody();
		assertEquals("failed", result.get("status"));
		assertEquals("Failed to trigger hero rankings update job - insufficient API requests or other constraints",
				result.get("message"));

		verify(batchSchedulerService).triggerHeroRankingUpdate();
	}

	@Test
	void testGetApiRateLimitStatus_ValidEndpoint_ShouldReturnStatus() {
		// Given
		String endpoint = "heroes";
		ApiRateLimit rateLimit = new ApiRateLimit();
		rateLimit.setEndpoint(endpoint);
		rateLimit.setRequestsCount(10);
		when(openDotaApiService.getRateLimitStatus(endpoint)).thenReturn(Optional.of(rateLimit));
		when(openDotaApiService.getRemainingMinuteRequests(endpoint)).thenReturn(50);

		// When
		ResponseEntity<Map<String, Object>> response = controller.getApiRateLimitStatus(endpoint);

		// Then
		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertNotNull(response.getBody());

		Map<String, Object> result = response.getBody();
		assertEquals(endpoint, result.get("endpoint"));
		assertEquals(50, result.get("remainingMinuteRequests"));
		assertNotNull(result.get("rateLimitStatus"));
		assertEquals(endpoint, ((ApiRateLimit) result.get("rateLimitStatus")).getEndpoint());

		verify(openDotaApiService).getRateLimitStatus(endpoint);
		verify(openDotaApiService).getRemainingMinuteRequests(endpoint);
	}

	@Test
	void testGetApiRateLimitStatus_NoRateLimitStatus_ShouldHandleGracefully() {
		// Given
		String endpoint = "heroes";
		when(openDotaApiService.getRateLimitStatus(endpoint)).thenReturn(Optional.empty());
		when(openDotaApiService.getRemainingMinuteRequests(endpoint)).thenReturn(50);

		// When
		ResponseEntity<Map<String, Object>> response = controller.getApiRateLimitStatus(endpoint);

		// Then
		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertNotNull(response.getBody());

		Map<String, Object> result = response.getBody();
		assertEquals(endpoint, result.get("endpoint"));
		assertEquals(50, result.get("remainingMinuteRequests"));
		assertNull(result.get("rateLimitStatus"));
	}

	@Test
	void testGetApiRateLimitStatus_Exception_ShouldReturnErrorResponse() {
		// Given
		String endpoint = "heroes";
		when(openDotaApiService.getRateLimitStatus(endpoint)).thenThrow(new RuntimeException("Service error"));

		// When
		ResponseEntity<Map<String, Object>> response = controller.getApiRateLimitStatus(endpoint);

		// Then
		assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
		assertNotNull(response.getBody());

		Map<String, Object> result = response.getBody();
		assertEquals("error", result.get("status"));
		assertNotNull(result.get("message"));
	}

	@Test
	void testGetApiRateLimitStatus_EmptyEndpoint_ShouldHandleGracefully() {
		// Given
		String endpoint = "";

		// When
		ResponseEntity<Map<String, Object>> response = controller.getApiRateLimitStatus(endpoint);

		// Then
		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertNotNull(response.getBody());

		Map<String, Object> result = response.getBody();
		assertEquals(endpoint, result.get("endpoint"));
	}

	@Test
	void testGetApiRateLimitStatus_NullEndpoint_ShouldHandleGracefully() {
		// Given
		String endpoint = null;

		// When
		ResponseEntity<Map<String, Object>> response = controller.getApiRateLimitStatus(endpoint);

		// Then
		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertNotNull(response.getBody());

		Map<String, Object> result = response.getBody();
		assertNull(result.get("endpoint"));
	}

	@Test
	void testAllEndpoints_ShouldReturnProperResponseStructure() {
		// Given
		when(batchSchedulerService.triggerHeroesUpdate()).thenReturn(true);
		when(batchSchedulerService.triggerPlayerUpdate()).thenReturn(true);
		when(batchSchedulerService.triggerTeamsUpdate()).thenReturn(true);
		when(batchSchedulerService.triggerNotablePlayerUpdate()).thenReturn(true);
		when(batchSchedulerService.triggerHeroRankingUpdate()).thenReturn(true);

		// When & Then - Test all trigger endpoints return proper structure
		ResponseEntity<Map<String, Object>> heroesResponse = controller.triggerHeroesUpdate();
		ResponseEntity<Map<String, Object>> playersResponse = controller.triggerPlayersUpdate();
		ResponseEntity<Map<String, Object>> teamsResponse = controller.triggerTeamsUpdate();
		ResponseEntity<Map<String, Object>> notablePlayersResponse = controller.triggerNotablePlayersUpdate();
		ResponseEntity<Map<String, Object>> heroRankingsResponse = controller.triggerHeroRankingsUpdate();

		// Verify all responses have proper structure
		assertAll(() -> assertNotNull(heroesResponse.getBody().get("status")),
				() -> assertNotNull(heroesResponse.getBody().get("message")),
				() -> assertNotNull(playersResponse.getBody().get("status")),
				() -> assertNotNull(playersResponse.getBody().get("message")),
				() -> assertNotNull(teamsResponse.getBody().get("status")),
				() -> assertNotNull(teamsResponse.getBody().get("message")),
				() -> assertNotNull(notablePlayersResponse.getBody().get("status")),
				() -> assertNotNull(notablePlayersResponse.getBody().get("message")),
				() -> assertNotNull(heroRankingsResponse.getBody().get("status")),
				() -> assertNotNull(heroRankingsResponse.getBody().get("message")));
	}

}
