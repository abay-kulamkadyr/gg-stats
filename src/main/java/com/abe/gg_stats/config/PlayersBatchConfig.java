package com.abe.gg_stats.config;

import com.abe.gg_stats.batch.listener.PlayersStepExecutionListener;
import com.abe.gg_stats.batch.player.PlayerReader;
import com.abe.gg_stats.batch.player.PlayerProcessor;
import com.abe.gg_stats.batch.player.PlayerWriter;
import com.abe.gg_stats.entity.Player;
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
public class PlayersBatchConfig {

	private final JobRepository jobRepository;

	private final PlatformTransactionManager transactionManager;

	@Value("${app.batch.players.chunk-size:100}")
	private int chunkSize;

	@Value("${app.batch.players.retry-limit:3}")
	private int retryLimit;

	@Value("${app.batch.players.skip-limit:10}")
	private int skipLimit;

	@Bean("playerUpdateJob")
	public Job playerUpdateJob(Step playerStep) {
		return new JobBuilder("playerUpdateJob", jobRepository).incrementer(new RunIdIncrementer())
			.start(playerStep)
			.build();
	}

	@Bean("playerStep")
	public Step playerStep(PlayerReader playerReader, PlayerProcessor playerProcessor, PlayerWriter playerWriter) {
		return new StepBuilder("playerStep", jobRepository).<JsonNode, Player>chunk(chunkSize, transactionManager)
			.reader(playerReader)
			.processor(playerProcessor)
			.writer(playerWriter)
			.faultTolerant()
			.retry(Exception.class)
			.retryLimit(retryLimit)
			.skip(Exception.class)
			.skipLimit(skipLimit)
			.listener(new PlayersStepExecutionListener())
			.build();
	}

}
