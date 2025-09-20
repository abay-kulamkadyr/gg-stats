package com.abe.gg_stats.config.batch;

import com.abe.gg_stats.batch.listener.BaseItemExecutionListener;
import com.abe.gg_stats.batch.listener.BaseJobExecutionListener;
import com.abe.gg_stats.batch.listener.BaseStepExecutionListener;
import com.abe.gg_stats.batch.match.MatchDetailWriter;
import com.abe.gg_stats.batch.match.NewProMatchesReader;
import com.abe.gg_stats.batch.match.ProMatchesToDetailProcessor;
import com.abe.gg_stats.exception.CircuitBreakerException;
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
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;

@Configuration
public class NewMatchesJobConfig {

	@Autowired
	private JobRepository jobRepository;

	@Autowired
	private PlatformTransactionManager transactionManager;

	@Value("${app.batch.promatches.new.chunk-size:10}")
	private int newChunkSize;

	@Bean("newMatchesIngestionJob")
	public Job newMatchesIngestionJob(Step newMatchesStep) {
		return new JobBuilder("newMatchesIngestionJob", jobRepository) //
			.incrementer(new RunIdIncrementer())
			.start(newMatchesStep)
			.listener(new BaseJobExecutionListener())
			.build();
	}

	@Bean("newMatchesStep")
	public Step newMatchesStep(NewProMatchesReader reader, ProMatchesToDetailProcessor processor,
			MatchDetailWriter writer) {
		ExponentialBackOffPolicy backoff = new ExponentialBackOffPolicy();
		backoff.setInitialInterval(1000);
		backoff.setMaxInterval(10000);
		backoff.setMultiplier(2.0);
		var itemListener = new BaseItemExecutionListener<JsonNode, JsonNode>();
		return new StepBuilder("newMatchesStep", jobRepository)
			.<JsonNode, JsonNode>chunk(newChunkSize, transactionManager)
			.reader(reader)
			.processor(processor)
			.writer(writer)
			.faultTolerant()
			.retry(ResourceAccessException.class)
			.retryLimit(3)
			.backOffPolicy(backoff)
			.skip(HttpClientErrorException.class)
			.skip(CircuitBreakerException.class)
			.skipLimit(50)
			.listener(new BaseStepExecutionListener())
			.listener(itemListener)
			.build();
	}

}