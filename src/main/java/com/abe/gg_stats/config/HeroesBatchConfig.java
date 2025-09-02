package com.abe.gg_stats.config;

import com.abe.gg_stats.batch.hero.HeroProcessor;
import com.abe.gg_stats.batch.hero.HeroWriter;
import com.abe.gg_stats.batch.hero.HeroesReader;
import com.abe.gg_stats.batch.listener.HeroesJobExecutionListener;
import com.abe.gg_stats.batch.listener.HeroesStepExecutionListener;
import com.fasterxml.jackson.databind.JsonNode;
import com.abe.gg_stats.dto.HeroDto;
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
	public Job heroesUpdateJob(Step heroesStep, HeroesJobExecutionListener heroesJobExecutionListener) {
		return new JobBuilder("heroesUpdateJob", jobRepository).incrementer(new RunIdIncrementer())
			.start(heroesStep)
			.listener(heroesJobExecutionListener)
			.build();
	}

	@Bean("heroesStep")
	public Step heroesStep(HeroesReader heroesReader, HeroProcessor heroProcessor, HeroWriter heroWriter) {
		return new StepBuilder("heroesStep", jobRepository).<JsonNode, HeroDto>chunk(chunkSize, transactionManager)
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
