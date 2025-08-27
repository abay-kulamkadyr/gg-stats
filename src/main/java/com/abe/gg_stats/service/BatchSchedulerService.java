package com.abe.gg_stats.service;

import com.abe.gg_stats.util.LoggingUtils;
import com.abe.gg_stats.util.ServiceLogging;
import com.abe.gg_stats.util.StructuredLoggingContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.util.UUID;

@Service
@Slf4j
public class BatchSchedulerService implements ServiceLogging {

	private final JobLauncher jobLauncher;

	private final Job heroesUpdateJob;

	private final Job notablePlayersUpdateJob;

	private final Job teamsUpdateJob;

	private final Job heroRankingUpdateJob;

	private final Job playerUpdateJob;

	private final RateLimitingService rateLimitingService;

	// Constructor injection with qualifiers
	public BatchSchedulerService(JobLauncher jobLauncher, RateLimitingService rateLimitingService,
			@Qualifier("heroesUpdateJob") Job heroesUpdateJob,
			@Qualifier("proPlayersUpdateJob") Job notablePlayersUpdateJob,
			@Qualifier("teamsUpdateJob") Job teamsUpdateJob,
			@Qualifier("heroRankingUpdateJob") Job heroRankingUpdateJob,
			@Qualifier("playerUpdateJob") Job playerUpdateJob) {

		this.jobLauncher = jobLauncher;
		this.heroesUpdateJob = heroesUpdateJob;
		this.notablePlayersUpdateJob = notablePlayersUpdateJob;
		this.teamsUpdateJob = teamsUpdateJob;
		this.heroRankingUpdateJob = heroRankingUpdateJob;
		this.playerUpdateJob = playerUpdateJob;
		this.rateLimitingService = rateLimitingService;
	}

	/**
	 * Run heroes update job daily at 2 AM Heroes data changes infrequently, so daily
	 * updates are sufficient
	 */
	@Scheduled(cron = "0 0 2 * * *")
	public void runHeroesUpdateJob() {
		if (canRunJob()) {
			runJob(heroesUpdateJob, "Heroes Update");
		}
	}

	/**
	 * Run pro players update job every 6 hours Pro player data changes more frequently
	 * (team changes, new pros, etc.)
	 */
	@Scheduled(cron = "0 0 */6 * * *")
	public void runProPlayersUpdateJob() {
		if (canRunJob()) {
			runJob(notablePlayersUpdateJob, "Pro Players Update");
		}
	}

	/**
	 * Run players update job every 6 hours Pro player data changes more frequently
	 *
	 */
	@Scheduled(cron = "0 0 */6 * * *")
	public void runPlayerUpdateJob() {
		if (canRunJob()) {
			runJob(playerUpdateJob, "Player Update");
		}
	}

	/**
	 * Run teams update job every 4 hours Team ratings and match results change frequently
	 */
	@Scheduled(cron = "0 0 */4 * * *")
	public void runTeamsUpdateJob() {
		if (canRunJob()) {
			runJob(teamsUpdateJob, "Teams Update");
		}
	}

	@Scheduled(cron = "0 0 */2 * * *") // every 2 hours
	public void runHeroRankingJob() {
		if (canRunJob()) {
			runJob(heroRankingUpdateJob, "Hero Ranking Update");
		}
	}

	/**
	 * Manual trigger for heroes job
	 */
	public boolean triggerHeroesUpdate() {
		return executeWithLogging("Manual Heroes Update", () -> {
			if (!canRunJob()) {
				return false;
			}
			return runJob(heroesUpdateJob, "Manual Heroes Update");
		}, "jobType=heroes");
	}

	/**
	 * Manual trigger for player job (can be called via REST endpoint)
	 */
	public boolean triggerPlayerUpdate() {
		return executeWithLogging("Manual Player Update", () -> {
			if (!canRunJob()) {
				return false;
			}
			return runJob(playerUpdateJob, "Manual Player Update");
		}, "jobType=players");
	}

	/**
	 * Manual trigger for pro players job
	 */
	public boolean triggerNotablePlayerUpdate() {
		if (canRunJob()) {
			return runJob(notablePlayersUpdateJob, "Manual Pro Players Update");
		}
		return false;
	}

	/**
	 * Manual trigger for teams job
	 */
	public boolean triggerTeamsUpdate() {
		if (canRunJob()) {
			return runJob(teamsUpdateJob, "Manual Teams Update");
		}
		return false;
	}

	/**
	 * Manual trigger for hero ranking job
	 */
	public boolean triggerHeroRankingUpdate() {
		if (canRunJob()) {
			return runJob(heroRankingUpdateJob, "Manual Hero Ranking Update");
		}
		return false;
	}

	/**
	 * Check if we have enough API requests remaining to run a job
	 */
	private boolean canRunJob() {
		int remainingRequests = rateLimitingService.getStatus().remainingDailyRequests();
		LoggingUtils.logDebug("Remaining daily API requests: {}", remainingRequests);

		// Ensure we have at least 50 requests remaining before running any job
		if (remainingRequests < 50) {
			LoggingUtils.logWarning("Insufficient API requests remaining, skipping scheduled job", 
				"remainingRequests=" + remainingRequests, "threshold=50");
			return false;
		}

		return true;
	}

	/**
	 * Generic method to run a batch job with error handling
	 */
	private boolean runJob(Job job, String jobDescription) {
		try {
			String correlationId = UUID.randomUUID().toString();
			LoggingUtils.logOperationStart(jobDescription, 
				"job=" + job.getName(), 
				"correlationId=" + correlationId);

			// Create unique parameters for each run with correlation ID
			JobParameters jobParameters = new JobParametersBuilder()
				.addLong("timestamp", System.currentTimeMillis())
				.addString("correlationId", correlationId)
				.toJobParameters();

			jobLauncher.run(job, jobParameters);
			LoggingUtils.logOperationSuccess(jobDescription, 
				"job=" + job.getName(), 
				"correlationId=" + correlationId);
			return true;
		}
		catch (Exception e) {
			LoggingUtils.logOperationFailure(jobDescription, "Job execution failed", e);
			return false;
		}
	}

	/**
	 * Get status information about API usage and job scheduling
	 */
	public String getSchedulerStatus() {
		int remainingRequests = rateLimitingService.getStatus().remainingDailyRequests();
		return String.format("Scheduler Status - Remaining API requests: %d, Jobs enabled: %s", remainingRequests,
				canRunJob());
	}

}