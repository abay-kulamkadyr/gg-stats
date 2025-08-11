package com.abe.gg_stats.config;

import com.abe.gg_stats.batch.HeroProcessor;
import com.abe.gg_stats.batch.HeroWriter;
import com.abe.gg_stats.batch.HeroesReader;
import com.abe.gg_stats.batch.LeaderboardProcessor;
import com.abe.gg_stats.batch.LeaderboardReader;
import com.abe.gg_stats.batch.LeaderboardWriter;
import com.abe.gg_stats.batch.ProPlayerProcessor;
import com.abe.gg_stats.batch.ProPlayerWriter;
import com.abe.gg_stats.batch.ProPlayersReader;
import com.abe.gg_stats.batch.TeamProcessor;
import com.abe.gg_stats.batch.TeamWriter;
import com.abe.gg_stats.batch.TeamsReader;
import com.abe.gg_stats.entity.Hero;
import com.abe.gg_stats.entity.LeaderboardRank;
import com.abe.gg_stats.entity.ProPlayer;
import com.abe.gg_stats.entity.Team;
import com.abe.gg_stats.service.OpenDotaApiService;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemReader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@RequiredArgsConstructor
public class BatchConfiguration {

	private final JobRepository jobRepository;

	private final PlatformTransactionManager transactionManager;

	private final OpenDotaApiService openDotaApiService;

	// === Heroes Job ===
	@Bean
	public Job heroesUpdateJob(Step heroesStep) {
		return new JobBuilder("heroesUpdateJob", jobRepository).start(heroesStep).build();
	}

	@Bean
	public Step heroesStep(HeroesReader heroesReader, HeroProcessor heroProcessor, HeroWriter heroWriter) {
		return new StepBuilder("heroesStep", jobRepository).<JsonNode, Hero>chunk(10, transactionManager)
			.reader(heroesReader)
			.processor(heroProcessor)
			.writer(heroWriter)
			.build();
	}

	// @Bean
	// public ItemReader<JsonNode> heroesReader() {
	// return new HeroesReader(openDotaApiService);
	// }

	// === Pro Players Job ===
	@Bean
	public Job proPlayersUpdateJob(Step proPlayersStep) {
		return new JobBuilder("proPlayersUpdateJob", jobRepository).start(proPlayersStep).build();
	}

	@Bean
	public Step proPlayersStep(ProPlayersReader proPlayersReader, ProPlayerProcessor proPlayerProcessor,
			ProPlayerWriter proPlayerWriter) {
		return new StepBuilder("proPlayersStep", jobRepository).<JsonNode, ProPlayer>chunk(10, transactionManager)
			.reader(proPlayersReader)
			.processor(proPlayerProcessor)
			.writer(proPlayerWriter)
			.build();
	}

	// @Bean
	// public ItemReader<JsonNode> proPlayersReader() {
	// return new ProPlayersReader(openDotaApiService);
	// }

	// === Teams Job ===
	@Bean
	public Job teamsUpdateJob(Step teamsStep) {
		return new JobBuilder("teamsUpdateJob", jobRepository).start(teamsStep).build();
	}

	@Bean
	public Step teamsStep(TeamsReader teamsReader, TeamProcessor teamProcessor, TeamWriter teamWriter) {
		return new StepBuilder("teamsStep", jobRepository).<JsonNode, Team>chunk(10, transactionManager)
			.reader(teamsReader)
			.processor(teamProcessor)
			.writer(teamWriter)
			.build();
	}

	// @Bean
	// public ItemReader<JsonNode> teamsReader() {
	// return new TeamsReader(openDotaApiService);
	// }

	// === Leaderboard Job ===
	@Bean
	public Job leaderboardUpdateJob(Step leaderboardStep) {
		return new JobBuilder("leaderboardUpdateJob", jobRepository).start(leaderboardStep).build();
	}

	@Bean
	public Step leaderboardStep(LeaderboardReader leaderboardReader, LeaderboardProcessor leaderboardProcessor,
			LeaderboardWriter leaderboardWriter) {
		return new StepBuilder("leaderboardStep", jobRepository)
			.<JsonNode, LeaderboardRank>chunk(50, transactionManager)
			.reader(leaderboardReader)
			.processor(leaderboardProcessor)
			.writer(leaderboardWriter)
			.build();
	}

	@Bean
	public Job allJobs(JobRepository jobRepository, Step heroesStep, Step proPlayersStep, Step teamsStep,
			Step leaderboardStep) {
		return new JobBuilder("allJobs", jobRepository).start(heroesStep)
			.next(proPlayersStep)
			.next(teamsStep)
			.next(leaderboardStep)
			.build();
	}
	// @Bean
	// public ItemReader<JsonNode> leaderboardReader() {
	// return new LeaderboardReader(openDotaApiService);
	// }

}