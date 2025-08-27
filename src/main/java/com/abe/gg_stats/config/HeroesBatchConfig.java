package com.abe.gg_stats.config;

import com.abe.gg_stats.batch.hero.HeroProcessor;
import com.abe.gg_stats.batch.hero.HeroWriter;
import com.abe.gg_stats.batch.hero.HeroesReader;
import com.abe.gg_stats.batch.listener.HeroesStepExecutionListener;
import com.abe.gg_stats.entity.Hero;
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
public class HeroesBatchConfig {

	private final JobRepository jobRepository;

	private final PlatformTransactionManager transactionManager;

	@Value("${app.batch.heroes.chunk-size:10}")
	private int chunkSize;

	@Value("${app.batch.heroes.retry-limit:3}")
	private int retryLimit;

	@Value("${app.batch.heroes.skip-limit:10}")
	private int skipLimit;

	@Bean("heroesUpdateJob")
	public Job heroesUpdateJob(Step heroesStep) {
		return new JobBuilder("heroesUpdateJob", jobRepository).incrementer(new RunIdIncrementer())
			.start(heroesStep)
			.build();
	}

	@Bean("heroesStep")
	public Step heroesStep(HeroesReader heroesReader, HeroProcessor heroProcessor, HeroWriter heroWriter) {
		return new StepBuilder("heroesStep", jobRepository).<JsonNode, Hero>chunk(chunkSize, transactionManager)
			.reader(heroesReader)
			.processor(heroProcessor)
			.writer(heroWriter)
			.faultTolerant()
			.retry(Exception.class)
			.retryLimit(retryLimit)
			.skip(Exception.class)
			.skipLimit(skipLimit)
			.listener(new HeroesStepExecutionListener())
			.build();
	}

}
