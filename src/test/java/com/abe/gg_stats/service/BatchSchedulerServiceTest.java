package com.abe.gg_stats.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.abe.gg_stats.service.rate_limit.OpenDotaRateLimitingService;
import com.abe.gg_stats.service.rate_limit.OpenDotaRateLimitingService.RateLimitStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.JobParameters;

class BatchSchedulerServiceTest {

	private JobLauncher jobLauncher;

	private Job heroesJob;

	private Job proPlayersJob;

	private Job teamsJob;

	private Job heroRankingJob;

	private Job playerJob;

	private Job newMatchesJob;

	private Job historicalMatchesJob;

	private OpenDotaRateLimitingService rateLimit;

	private AggregationService aggregationService;

	private BatchSchedulerService service;

	@BeforeEach
	void setUp() {
		jobLauncher = mock(JobLauncher.class);
		heroesJob = mock(Job.class);
		proPlayersJob = mock(Job.class);
		teamsJob = mock(Job.class);
		heroRankingJob = mock(Job.class);
		playerJob = mock(Job.class);
		newMatchesJob = mock(Job.class);
		historicalMatchesJob = mock(Job.class);
		rateLimit = mock(OpenDotaRateLimitingService.class);
		aggregationService = mock(AggregationService.class);

		service = new BatchSchedulerService(jobLauncher, heroesJob, proPlayersJob, teamsJob, heroRankingJob, playerJob,
				newMatchesJob, historicalMatchesJob, rateLimit, aggregationService);
	}

	private RateLimitStatus statusWithRemaining(int remaining) {
		return RateLimitStatus.builder()
			.availableTokens(0)
			.remainingDailyRequests(remaining)
			.totalRequests(0)
			.rejectedRequests(0)
			.successRate(100.0)
			.timeUntilDailyReset(0)
			.pendingChanges(0)
			.lastSyncAge(0)
			.build();
	}

	@Test
	void triggerHeroesUpdateReturnsFalseWhenBelowThreshold() {
		when(rateLimit.getStatus()).thenReturn(statusWithRemaining(10));
		boolean res = service.triggerHeroesUpdate();
		assertFalse(res);
		verifyNoInteractions(jobLauncher);
	}

	@Test
	void triggerHeroesUpdateRunsWhenAboveThreshold() throws Exception {
		when(rateLimit.getStatus()).thenReturn(statusWithRemaining(100));
		// jobLauncher.run will be called; no exception -> true
		boolean res = service.triggerHeroesUpdate();
		assertTrue(res);
		verify(jobLauncher).run(any(Job.class), any(JobParameters.class));
	}

	@Test
	void runAggregationsInvokesServiceWhenAllowed() {
		when(rateLimit.getStatus()).thenReturn(statusWithRemaining(100));
		service.runAggregations();
		verify(aggregationService).refreshPatchesAndAggregations();
	}

	@Test
	void runAggregationsSkipsWhenLowTokens() {
		when(rateLimit.getStatus()).thenReturn(statusWithRemaining(1));
		service.runAggregations();
		verifyNoInteractions(aggregationService);
	}

}
