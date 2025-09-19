package com.abe.gg_stats.config.batch;

import com.abe.gg_stats.batch.listener.BaseItemExecutionListener;
import com.abe.gg_stats.batch.listener.BaseJobExecutionListener;
import com.abe.gg_stats.batch.listener.MatchesStepExecutionListener;
import com.abe.gg_stats.batch.match.HistoricalProMatchesReader;
import com.abe.gg_stats.batch.match.MatchDetailWriter;
import com.abe.gg_stats.batch.match.ProMatchesToDetailProcessor;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.web.client.ResourceAccessException;

@Configuration
public class HistoricalMatchesJobConfig {

	@Autowired
	private JobRepository jobRepository;

	@Autowired
	private PlatformTransactionManager transactionManager;

	@Value("${app.batch.promatches.historical.chunk-size:20}")
	private int historicalChunkSize;

	@Bean("historicalMatchesIngestionJob")
	public Job historicalMatchesIngestionJob(Step fetchHistoricalMatchesStep) {
		return new JobBuilder("historicalMatchesIngestionJob", jobRepository) //
			.incrementer(new RunIdIncrementer())
			.start(fetchHistoricalMatchesStep)
			.listener(new BaseJobExecutionListener())
			.build();
	}

	@Bean("fetchHistoricalMatchesStep")
	public Step fetchHistoricalMatchesStep(HistoricalProMatchesReader reader, ProMatchesToDetailProcessor processor,
			MatchDetailWriter writer) {
		ExponentialBackOffPolicy backoff = new ExponentialBackOffPolicy();
		backoff.setInitialInterval(1000);
		backoff.setMaxInterval(10000);
		backoff.setMultiplier(2.0);
		var itemListener = new BaseItemExecutionListener<JsonNode, JsonNode>();
		return new StepBuilder("fetchHistoricalMatchesStep", jobRepository)
			.<JsonNode, JsonNode>chunk(historicalChunkSize, transactionManager)
			.reader(reader)
			.processor(processor)
			.writer(writer)
			.faultTolerant()
			.retry(ResourceAccessException.class)
			.retryLimit(3)
			.backOffPolicy(backoff)
			.skip(Exception.class)
			.skipLimit(15)
			.listener(new MatchesStepExecutionListener())
			.listener(itemListener)
			.build();
	}

}