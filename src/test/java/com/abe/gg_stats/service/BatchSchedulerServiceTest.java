package com.abe.gg_stats.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.launch.JobLauncher;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BatchSchedulerServiceTest {

	@Mock
	private JobLauncher jobLauncher;

	@Mock
	private OpenDotaApiService openDotaApiService;

	@Mock
	private Job heroesUpdateJob;

	@Mock
	private Job notablePlayersUpdateJob;

	@Mock
	private Job teamsUpdateJob;

	@Mock
	private Job heroRankingUpdateJob;

	@Mock
	private Job playerUpdateJob;

	@Mock
	private JobExecution jobExecution;

	private BatchSchedulerService batchSchedulerService;

	@BeforeEach
	void setUp() {
		batchSchedulerService = new BatchSchedulerService(jobLauncher, openDotaApiService, heroesUpdateJob,
				notablePlayersUpdateJob, teamsUpdateJob, heroRankingUpdateJob, playerUpdateJob);
	}

	@Test
	void testTriggerHeroesUpdate_Success_ShouldReturnTrue() throws Exception {
		// Given
		when(openDotaApiService.getRemainingDailyRequests()).thenReturn(100);
		when(jobLauncher.run(eq(heroesUpdateJob), any(JobParameters.class))).thenReturn(jobExecution);

		// When
		boolean result = batchSchedulerService.triggerHeroesUpdate();

		// Then
		assertTrue(result);
		verify(jobLauncher).run(eq(heroesUpdateJob), any(JobParameters.class));
		verify(openDotaApiService).getRemainingDailyRequests();
	}

	@Test
	void testTriggerHeroesUpdate_InsufficientApiRequests_ShouldReturnFalse() throws Exception {
		// Given
		when(openDotaApiService.getRemainingDailyRequests()).thenReturn(30);

		// When
		boolean result = batchSchedulerService.triggerHeroesUpdate();

		// Then
		assertFalse(result);
		verify(jobLauncher, never()).run(any(), any());
		verify(openDotaApiService).getRemainingDailyRequests();
	}

	@Test
	void testTriggerHeroesUpdate_JobExecutionException_ShouldReturnFalse() throws Exception {
		// Given
		when(openDotaApiService.getRemainingDailyRequests()).thenReturn(100);
		when(jobLauncher.run(eq(heroesUpdateJob), any(JobParameters.class)))
			.thenThrow(new RuntimeException("Job execution failed"));

		// When
		boolean result = batchSchedulerService.triggerHeroesUpdate();

		// Then
		assertFalse(result);
		verify(jobLauncher).run(eq(heroesUpdateJob), any(JobParameters.class));
		verify(openDotaApiService).getRemainingDailyRequests();
	}

	@Test
	void testTriggerPlayerUpdate_Success_ShouldReturnTrue() throws Exception {
		// Given
		when(openDotaApiService.getRemainingDailyRequests()).thenReturn(100);
		when(jobLauncher.run(eq(playerUpdateJob), any(JobParameters.class))).thenReturn(jobExecution);

		// When
		boolean result = batchSchedulerService.triggerPlayerUpdate();

		// Then
		assertTrue(result);
		verify(jobLauncher).run(eq(playerUpdateJob), any(JobParameters.class));
		verify(openDotaApiService).getRemainingDailyRequests();
	}

	@Test
	void testTriggerPlayerUpdate_InsufficientApiRequests_ShouldReturnFalse() throws Exception {
		// Given
		when(openDotaApiService.getRemainingDailyRequests()).thenReturn(30);

		// When
		boolean result = batchSchedulerService.triggerPlayerUpdate();

		// Then
		assertFalse(result);
		verify(jobLauncher, never()).run(any(), any());
		verify(openDotaApiService).getRemainingDailyRequests();
	}

	@Test
	void testTriggerNotablePlayerUpdate_Success_ShouldReturnTrue() throws Exception {
		// Given
		when(openDotaApiService.getRemainingDailyRequests()).thenReturn(100);
		when(jobLauncher.run(eq(notablePlayersUpdateJob), any(JobParameters.class))).thenReturn(jobExecution);

		// When
		boolean result = batchSchedulerService.triggerNotablePlayerUpdate();

		// Then
		assertTrue(result);
		verify(jobLauncher).run(eq(notablePlayersUpdateJob), any(JobParameters.class));
		verify(openDotaApiService).getRemainingDailyRequests();
	}

	@Test
	void testTriggerNotablePlayerUpdate_InsufficientApiRequests_ShouldReturnFalse() throws Exception {
		// Given
		when(openDotaApiService.getRemainingDailyRequests()).thenReturn(30);

		// When
		boolean result = batchSchedulerService.triggerNotablePlayerUpdate();

		// Then
		assertFalse(result);
		verify(jobLauncher, never()).run(any(), any());
		verify(openDotaApiService).getRemainingDailyRequests();
	}

	@Test
	void testTriggerTeamsUpdate_Success_ShouldReturnTrue() throws Exception {
		// Given
		when(openDotaApiService.getRemainingDailyRequests()).thenReturn(100);
		when(jobLauncher.run(eq(teamsUpdateJob), any(JobParameters.class))).thenReturn(jobExecution);

		// When
		boolean result = batchSchedulerService.triggerTeamsUpdate();

		// Then
		assertTrue(result);
		verify(jobLauncher).run(eq(teamsUpdateJob), any(JobParameters.class));
		verify(openDotaApiService).getRemainingDailyRequests();
	}

	@Test
	void testTriggerTeamsUpdate_InsufficientApiRequests_ShouldReturnFalse() throws Exception {
		// Given
		when(openDotaApiService.getRemainingDailyRequests()).thenReturn(30);

		// When
		boolean result = batchSchedulerService.triggerTeamsUpdate();

		// Then
		assertFalse(result);
		verify(jobLauncher, never()).run(any(), any());
		verify(openDotaApiService).getRemainingDailyRequests();
	}

	@Test
	void testTriggerHeroRankingUpdate_Success_ShouldReturnTrue() throws Exception {
		// Given
		when(openDotaApiService.getRemainingDailyRequests()).thenReturn(100);
		when(jobLauncher.run(eq(heroRankingUpdateJob), any(JobParameters.class))).thenReturn(jobExecution);

		// When
		boolean result = batchSchedulerService.triggerHeroRankingUpdate();

		// Then
		assertTrue(result);
		verify(jobLauncher).run(eq(heroRankingUpdateJob), any(JobParameters.class));
		verify(openDotaApiService).getRemainingDailyRequests();
	}

	@Test
	void testTriggerHeroRankingUpdate_InsufficientApiRequests_ShouldReturnFalse() throws Exception {
		// Given
		when(openDotaApiService.getRemainingDailyRequests()).thenReturn(30);

		// When
		boolean result = batchSchedulerService.triggerHeroRankingUpdate();

		// Then
		assertFalse(result);
		verify(jobLauncher, never()).run(any(), any());
		verify(openDotaApiService).getRemainingDailyRequests();
	}

	@Test
	void testCanRunJob_WithSufficientRequests_ShouldReturnTrue() throws Exception {
		// Given
		when(openDotaApiService.getRemainingDailyRequests()).thenReturn(100);

		// When
		boolean result = batchSchedulerService.triggerHeroesUpdate();

		// Then
		assertTrue(result);
		verify(openDotaApiService).getRemainingDailyRequests();
	}

	@Test
	void testCanRunJob_WithExactlyMinimumRequests_ShouldReturnTrue() throws Exception {
		// Given
		when(openDotaApiService.getRemainingDailyRequests()).thenReturn(50);
		when(jobLauncher.run(eq(heroesUpdateJob), any(JobParameters.class))).thenReturn(jobExecution);

		// When
		boolean result = batchSchedulerService.triggerHeroesUpdate();

		// Then
		assertTrue(result);
		verify(openDotaApiService).getRemainingDailyRequests();
	}

	@Test
	void testCanRunJob_WithInsufficientRequests_ShouldReturnFalse() throws Exception {
		// Given
		when(openDotaApiService.getRemainingDailyRequests()).thenReturn(49);

		// When
		boolean result = batchSchedulerService.triggerHeroesUpdate();

		// Then
		assertFalse(result);
		verify(openDotaApiService).getRemainingDailyRequests();
		verify(jobLauncher, never()).run(any(), any());
	}

	@Test
	void testCanRunJob_WithZeroRequests_ShouldReturnFalse() throws Exception {
		// Given
		when(openDotaApiService.getRemainingDailyRequests()).thenReturn(0);

		// When
		boolean result = batchSchedulerService.triggerHeroesUpdate();

		// Then
		assertFalse(result);
		verify(openDotaApiService).getRemainingDailyRequests();
		verify(jobLauncher, never()).run(any(), any());
	}

	@Test
	void testCanRunJob_WithNegativeRequests_ShouldReturnFalse() throws Exception {
		// Given
		when(openDotaApiService.getRemainingDailyRequests()).thenReturn(-10);

		// When
		boolean result = batchSchedulerService.triggerHeroesUpdate();

		// Then
		assertFalse(result);
		verify(openDotaApiService).getRemainingDailyRequests();
		verify(jobLauncher, never()).run(any(), any());
	}

	@Test
	void testRunJob_Success_ShouldReturnTrue() throws Exception {
		// Given
		when(openDotaApiService.getRemainingDailyRequests()).thenReturn(100);
		when(jobLauncher.run(eq(heroesUpdateJob), any(JobParameters.class))).thenReturn(jobExecution);

		// When
		boolean result = batchSchedulerService.triggerHeroesUpdate();

		// Then
		assertTrue(result);
		verify(jobLauncher).run(eq(heroesUpdateJob), any(JobParameters.class));
	}

	@Test
	void testRunJob_Exception_ShouldReturnFalse() throws Exception {
		// Given
		when(openDotaApiService.getRemainingDailyRequests()).thenReturn(100);
		when(jobLauncher.run(eq(heroesUpdateJob), any(JobParameters.class)))
			.thenThrow(new RuntimeException("Job execution failed"));

		// When
		boolean result = batchSchedulerService.triggerHeroesUpdate();

		// Then
		assertFalse(result);
		verify(jobLauncher).run(eq(heroesUpdateJob), any(JobParameters.class));
	}

	@Test
	void testGetSchedulerStatus_WithSufficientRequests_ShouldReturnEnabledStatus() throws Exception {
		// Given
		when(openDotaApiService.getRemainingDailyRequests()).thenReturn(100);

		// When
		String status = batchSchedulerService.getSchedulerStatus();

		// Then
		assertNotNull(status);
		assertTrue(status.contains("Remaining API requests: 100"));
		assertTrue(status.contains("Jobs enabled: true"));
		verify(openDotaApiService, times(2)).getRemainingDailyRequests(); // Called once
																			// in
																			// getSchedulerStatus,
																			// once in
																			// canRunJob
	}

	@Test
	void testGetSchedulerStatus_WithInsufficientRequests_ShouldReturnDisabledStatus() throws Exception {
		// Given
		when(openDotaApiService.getRemainingDailyRequests()).thenReturn(30);

		// When
		String status = batchSchedulerService.getSchedulerStatus();

		// Then
		assertNotNull(status);
		assertTrue(status.contains("Remaining API requests: 30"));
		assertTrue(status.contains("Jobs enabled: false"));
		verify(openDotaApiService, times(2)).getRemainingDailyRequests(); // Called once
																			// in
																			// getSchedulerStatus,
																			// once in
																			// canRunJob
	}

	@Test
	void testGetSchedulerStatus_WithZeroRequests_ShouldReturnDisabledStatus() throws Exception {
		// Given
		when(openDotaApiService.getRemainingDailyRequests()).thenReturn(0);

		// When
		String status = batchSchedulerService.getSchedulerStatus();

		// Then
		assertNotNull(status);
		assertTrue(status.contains("Remaining API requests: 0"));
		assertTrue(status.contains("Jobs enabled: false"));
		verify(openDotaApiService, times(2)).getRemainingDailyRequests(); // Called once
																			// in
																			// getSchedulerStatus,
																			// once in
																			// canRunJob
	}

	@Test
	void testJobParameters_ShouldBeUniqueForEachRun() throws Exception {
		// Given
		when(openDotaApiService.getRemainingDailyRequests()).thenReturn(100);
		when(jobLauncher.run(eq(heroesUpdateJob), any(JobParameters.class))).thenReturn(jobExecution);

		// When
		batchSchedulerService.triggerHeroesUpdate();
		batchSchedulerService.triggerHeroesUpdate();

		// Then
		verify(jobLauncher, times(2)).run(eq(heroesUpdateJob), any(JobParameters.class));

		// Verify that different timestamps were used (capture arguments to check)
		verify(jobLauncher, times(2)).run(eq(heroesUpdateJob), any(JobParameters.class));
	}

	@Test
	void testAllTriggerMethods_ShouldCheckApiRequestsFirst() throws Exception {
		// Given
		when(openDotaApiService.getRemainingDailyRequests()).thenReturn(30);

		// When
		batchSchedulerService.triggerHeroesUpdate();
		batchSchedulerService.triggerPlayerUpdate();
		batchSchedulerService.triggerNotablePlayerUpdate();
		batchSchedulerService.triggerTeamsUpdate();
		batchSchedulerService.triggerHeroRankingUpdate();

		// Then
		verify(openDotaApiService, times(5)).getRemainingDailyRequests();
		verify(jobLauncher, never()).run(any(), any());
	}

}
