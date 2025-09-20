package com.abe.gg_stats.config.batch;

import com.abe.gg_stats.batch.listener.BaseItemExecutionListener;
import com.abe.gg_stats.batch.listener.BaseJobExecutionListener;
import com.abe.gg_stats.batch.listener.BaseStepExecutionListener;
import com.abe.gg_stats.batch.team.TeamProcessor;
import com.abe.gg_stats.batch.team.TeamWriter;
import com.abe.gg_stats.batch.team.TeamsReader;
import com.abe.gg_stats.dto.request.opendota.OpenDotaTeamDto;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
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
		return new JobBuilder("teamsUpdateJob", jobRepository) //
			.incrementer(new RunIdIncrementer())
			.start(teamsStep)
			.listener(new BaseJobExecutionListener())
			.build();
	}

	@Bean("teamsStep")
	Step teamsStep(TeamsReader teamsReader, TeamProcessor teamProcessor, TeamWriter teamWriter) {
		var itemListener = new BaseItemExecutionListener<JsonNode, OpenDotaTeamDto>();
		return new StepBuilder("teamsStep", jobRepository) //
			.<JsonNode, OpenDotaTeamDto>chunk(chunkSize, transactionManager)
			.reader(teamsReader)
			.processor(teamProcessor)
			.writer(teamWriter)
			.faultTolerant()
			.retry(Exception.class)
			.retryLimit(retryLimit)
			.skip(Exception.class)
			.skipLimit(skipLimit)
			.listener(new BaseStepExecutionListener())
			.listener(itemListener)
			.build();
	}

}
