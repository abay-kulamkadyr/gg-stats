package com.abe.gg_stats.config;

import com.abe.gg_stats.batch.HeroRankingProcessor;
import com.abe.gg_stats.batch.HeroRankingReader;
import com.abe.gg_stats.batch.HeroRankingWriter;
import com.abe.gg_stats.batch.listener.HeroRankingsStepExecutionListener;
import com.abe.gg_stats.entity.HeroRanking;
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
public class HeroRankingsBatchConfig {

	private final JobRepository jobRepository;

	private final PlatformTransactionManager transactionManager;

	@Value("${app.batch.hero-rankings.chunk-size:10}")
	private int chunkSize;

	@Value("${app.batch.hero-rankings.retry-limit:3}")
	private int retryLimit;

	@Value("${app.batch.hero-rankings.skip-limit:10}")
	private int skipLimit;

	@Bean("heroRankingUpdateJob")
	public Job heroRankingUpdateJob(Step heroRankingStep) {
		return new JobBuilder("heroRankingUpdateJob", jobRepository).incrementer(new RunIdIncrementer())
			.start(heroRankingStep)
			.build();
	}

	@Bean("heroRankingStep")
	public Step heroRankingStep(HeroRankingReader heroRankingReader, HeroRankingProcessor heroRankingProcessor,
			HeroRankingWriter heroRankingWriter) {
		return new StepBuilder("heroRankingStep", jobRepository)
			.<JsonNode, HeroRanking>chunk(chunkSize, transactionManager)
			.reader(heroRankingReader)
			.processor(heroRankingProcessor)
			.writer(heroRankingWriter)
			.faultTolerant()
			.retry(Exception.class)
			.retryLimit(retryLimit)
			.skip(Exception.class)
			.skipLimit(skipLimit)
			.listener(new HeroRankingsStepExecutionListener())
			.build();
	}

}
