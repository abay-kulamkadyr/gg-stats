package com.abe.gg_stats.config;

import com.abe.gg_stats.batch.match.MatchDetailWriter;
import com.abe.gg_stats.batch.match.ProMatchesReader;
import com.abe.gg_stats.batch.match.ProMatchesToDetailProcessor;
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
public class ProMatchesBatchConfig {

	private final JobRepository jobRepository;

	private final PlatformTransactionManager transactionManager;

	@Value("${app.batch.promatches.chunk-size:5}")
	private int chunkSize;

	@Bean("proMatchesJob")
	public Job proMatchesJob(Step proMatchesStep) {
		return new JobBuilder("proMatchesJob", jobRepository).incrementer(new RunIdIncrementer())
			.start(proMatchesStep)
			.build();
	}

	@Bean
	public Step proMatchesStep(ProMatchesReader reader, ProMatchesToDetailProcessor processor,
			MatchDetailWriter writer) {
		return new StepBuilder("proMatchesStep", jobRepository).<JsonNode, JsonNode>chunk(chunkSize, transactionManager)
			.reader(reader)
			.processor(processor)
			.writer(writer)
			.build();
	}

}
