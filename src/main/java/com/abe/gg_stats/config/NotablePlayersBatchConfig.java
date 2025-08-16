package com.abe.gg_stats.config;

import com.abe.gg_stats.batch.player.notablePlayer.NotablePlayerProcessor;
import com.abe.gg_stats.batch.player.notablePlayer.NotablePlayerWriter;
import com.abe.gg_stats.batch.player.notablePlayer.NotablePlayersReader;
import com.abe.gg_stats.batch.listener.NotablePlayersStepExecutionListener;
import com.abe.gg_stats.entity.NotablePlayer;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class NotablePlayersBatchConfig {

	private final JobRepository jobRepository;

	private final PlatformTransactionManager transactionManager;

	@Value("${app.batch.notable-players.chunk-size:10}")
	private int chunkSize;

	@Value("${app.batch.notable-players.retry-limit:3}")
	private int retryLimit;

	@Value("${app.batch.notable-players.skip-limit:10}")
	private int skipLimit;

	@Bean("proPlayersUpdateJob")
	public Job proPlayersUpdateJob(Step proPlayersStep) {
		return new JobBuilder("proPlayersUpdateJob", jobRepository).start(proPlayersStep).build();
	}

	@Bean("proPlayersStep")
	public Step proPlayersStep(NotablePlayersReader proPlayersReader, NotablePlayerProcessor notablePlayerProcessor,
			NotablePlayerWriter notablePlayerWriter) {
		return new StepBuilder("proPlayersStep", jobRepository)
			.<JsonNode, NotablePlayer>chunk(chunkSize, transactionManager)
			.reader(proPlayersReader)
			.processor(notablePlayerProcessor)
			.writer(notablePlayerWriter)
			.faultTolerant()
			.retry(Exception.class)
			.retryLimit(retryLimit)
			.skip(Exception.class)
			.skipLimit(skipLimit)
			.listener(new NotablePlayersStepExecutionListener())
			.build();
	}

}
