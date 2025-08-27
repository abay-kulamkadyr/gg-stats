package com.abe.gg_stats.config;

import com.abe.gg_stats.batch.team.TeamProcessor;
import com.abe.gg_stats.batch.team.TeamWriter;
import com.abe.gg_stats.batch.team.TeamsReader;
import com.abe.gg_stats.batch.listener.TeamsStepExecutionListener;
import com.abe.gg_stats.entity.Team;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class TeamsBatchConfig {

	private final JobRepository jobRepository;

	private final PlatformTransactionManager transactionManager;

	@Value("${app.batch.teams.chunk-size:10}")
	private int chunkSize;

	@Value("${app.batch.teams.retry-limit:3}")
	private int retryLimit;

	@Value("${app.batch.teams.skip-limit:10}")
	private int skipLimit;

	@Bean("teamsUpdateJob")
	public Job teamsUpdateJob(Step teamsStep) {
		return new JobBuilder("teamsUpdateJob", jobRepository).incrementer(new RunIdIncrementer())
			.start(teamsStep)
			.build();
	}

	@Bean("teamsStep")
	public Step teamsStep(TeamsReader teamsReader, TeamProcessor teamProcessor, TeamWriter teamWriter) {
		return new StepBuilder("teamsStep", jobRepository).<JsonNode, Team>chunk(chunkSize, transactionManager)
			.reader(teamsReader)
			.processor(teamProcessor)
			.writer(teamWriter)
			.faultTolerant()
			.retry(Exception.class)
			.retryLimit(retryLimit)
			.skip(Exception.class)
			.skipLimit(skipLimit)
			.listener(new TeamsStepExecutionListener())
			.build();
	}

}
