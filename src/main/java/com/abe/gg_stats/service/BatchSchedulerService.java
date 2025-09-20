package com.abe.gg_stats.service;

import com.abe.gg_stats.service.rate_limit.OpenDotaRateLimitingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class BatchSchedulerService {

	private final JobLauncher jobLauncher;

	private final Job heroesUpdateJob;

	private final Job notablePlayersUpdateJob;

	private final Job teamsUpdateJob;

	private final Job heroRankingUpdateJob;

	private final Job playerUpdateJob;

	private final OpenDotaRateLimitingService openDotaRateLimitingService;

	private final AggregationService aggregationService;

	private final Job newMatchesIngestionJob;

	private final Job historicalMatchesIngestionJob;

	public BatchSchedulerService(JobLauncher jobLauncher, //
			@Qualifier("heroesUpdateJob") Job heroesUpdateJob,
			@Qualifier("proPlayersUpdateJob") Job notablePlayersUpdateJob,
			@Qualifier("teamsUpdateJob") Job teamsUpdateJob,
			@Qualifier("heroRankingUpdateJob") Job heroRankingUpdateJob,
			@Qualifier("playerUpdateJob") Job playerUpdateJob,
			@Qualifier("newMatchesIngestionJob") Job newMatchesIngestionJob,
			@Qualifier("historicalMatchesIngestionJob") Job historicalMatchesIngestionJob,
			OpenDotaRateLimitingService openDotaRateLimitingService, AggregationService aggregationService) {
		this.jobLauncher = jobLauncher;
		this.heroesUpdateJob = heroesUpdateJob;
		this.notablePlayersUpdateJob = notablePlayersUpdateJob;
		this.teamsUpdateJob = teamsUpdateJob;
		this.heroRankingUpdateJob = heroRankingUpdateJob;
		this.playerUpdateJob = playerUpdateJob;
		this.newMatchesIngestionJob = newMatchesIngestionJob;
		this.historicalMatchesIngestionJob = historicalMatchesIngestionJob;
		this.aggregationService = aggregationService;
		this.openDotaRateLimitingService = openDotaRateLimitingService;
	}

	/**
	 * Run new matches ingestion job every 15 minutes. This high-frequency job is
	 * essential for keeping the data up-to-date with new game results. It's scheduled to
	 * run at the 0, 15, 30, and 45 minute marks of every hour.
	 */
	@Scheduled(cron = "0 */15 * * * *") // Every 15 minutes
	public void runNewMatchesIngestionJob() {
		if (canRunJob()) {
			runJob(newMatchesIngestionJob, "New Matches Ingestion");
		}
	}

	/**
	 * Run historical matches ingestion job daily at 3 AM. This job is resource-intensive
	 * and is scheduled after the heroes update to ensure it uses the latest hero data. It
	 * runs during off-peak hours to avoid performance degradation for other services.
	 */
	@Scheduled(cron = "0 0 3 * * *") // Daily at 3 AM
	public void runHistoricalMatchesIngestionJob() {
		if (canRunJob()) {
			runJob(historicalMatchesIngestionJob, "Historical Matches Ingestion");
		}
	}

	/**
	 * Run heroes update job daily at 2 AM. Hero data changes infrequently (e.g., with
	 * major game patches), so a daily update is sufficient. This job is scheduled for a
	 * low-traffic time to minimize impact on system performance.
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
	 * Run players update job every 6 hours. This job is for general player data updates
	 * and runs on the same schedule as the pro players update as their data is often
	 * related and can be processed together. It runs at 0, 6, 12, and 18 o'clock.
	 */
	@Scheduled(cron = "0 0 */6 * * *")
	public void runPlayerUpdateJob() {
		if (canRunJob()) {
			runJob(playerUpdateJob, "Player Update");
		}
	}

	/**
	 * Run teams update job every 4 hours. Team ratings and match results can change
	 * frequently. This schedule ensures the data stays fresh. It runs at 0, 4, 8, 12, 16,
	 * and 20 o'clock.
	 */
	@Scheduled(cron = "0 0 */4 * * *")
	public void runTeamsUpdateJob() {
		if (canRunJob()) {
			runJob(teamsUpdateJob, "Teams Update");
		}
	}

	/**
	 * Run hero ranking update job every 2 hours. This job updates player and hero
	 * rankings based on new match data. The frequency is a good balance between keeping
	 * rankings current and system load. It runs every two hours on the hour (e.g., 0, 2,
	 * 4, 6... o'clock).
	 */
	@Scheduled(cron = "0 0 */2 * * *") // every 2 hours
	public void runHeroRankingJob() {
		if (canRunJob()) {
			runJob(heroRankingUpdateJob, "Hero Ranking Update");
		}
	}

	/**
	 * Run aggregations daily at 5:15 AM. This job is responsible for final data
	 * aggregation and should run after all major ingestion jobs have completed (e.g., the
	 * historical matches job at 3 AM).
	 */
	@Scheduled(cron = "15 5 * * * *")
	public void runAggregations() {
		if (!canRunJob()) {
			return;
		}
		try {
			aggregationService.refreshPatchesAndAggregations();
		}
		catch (Exception e) {
			log.error("Aggregation Update Job failed, reason={}", e.toString());
		}
	}

	/**
	 * Manual trigger for heroes job
	 */
	public boolean triggerHeroesUpdate() {
		if (canRunJob()) {
			return runJob(heroesUpdateJob, "Heroes Update Job");
		}
		return false;
	}

	public boolean triggerPlayerUpdate() {
		if (canRunJob()) {
			return runJob(playerUpdateJob, "Player Update Job");
		}
		return false;
	}

	public boolean triggerNotablePlayerUpdate() {
		if (canRunJob()) {
			return runJob(notablePlayersUpdateJob, "Manual Pro Players Update");
		}
		return false;
	}

	public boolean triggerTeamsUpdate() {
		if (canRunJob()) {
			return runJob(teamsUpdateJob, "Manual Teams Update");
		}
		return false;
	}

	public boolean triggerHeroRankingUpdate() {
		if (canRunJob()) {
			return runJob(heroRankingUpdateJob, "Manual Hero Ranking Update");
		}
		return false;
	}

	public boolean triggerNewMatchesIngestion() {
		if (canRunJob()) {
			return runJob(newMatchesIngestionJob, "Manual New Matches Ingestion");
		}
		return false;
	}

	public boolean triggerHistoricalMatchesIngestion() {
		if (canRunJob()) {
			return runJob(historicalMatchesIngestionJob, "Manual Historical Matches Ingestion");
		}
		return false;
	}

	/**
	 * Check if we have enough API requests remaining to run a job
	 */
	private boolean canRunJob() {
		int remainingRequests = openDotaRateLimitingService.getStatus().remainingDailyRequests();

		int apiTokenThreshold = 50;
		if (remainingRequests < apiTokenThreshold) {
			log.warn("Insufficient API requests remaining, skipping batch job, remaining requests={}, threshold={}",
					remainingRequests, apiTokenThreshold);
			return false;
		}

		return true;
	}

	/**
	 * Generic method to run a batch job with error handling
	 */
	private boolean runJob(Job job, String jobDescription) {
		try {
			jobLauncher.run(job,
					new JobParametersBuilder().addLong("timestamp", System.currentTimeMillis()).toJobParameters());
			return true;
		}
		catch (Exception e) {
			log.error("BatchSchedulerService couldn't start for job={}, reason={}", jobDescription, e.toString());
			return false;
		}
	}

}