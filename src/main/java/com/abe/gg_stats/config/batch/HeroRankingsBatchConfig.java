package com.abe.gg_stats.config.batch;

import com.abe.gg_stats.batch.hero_ranking.HeroRankingProcessor;
import com.abe.gg_stats.batch.hero_ranking.HeroRankingReader;
import com.abe.gg_stats.batch.hero_ranking.HeroRankingWriter;
import com.abe.gg_stats.batch.listener.BaseItemExecutionListener;
import com.abe.gg_stats.batch.listener.BaseJobExecutionListener;
import com.abe.gg_stats.batch.listener.BaseStepExecutionListener;
import com.abe.gg_stats.dto.request.opendota.OpenDotaHeroRankingDto;
import java.util.List;
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
		return new JobBuilder("heroRankingUpdateJob", jobRepository)//
			.incrementer(new RunIdIncrementer())
			.start(heroRankingStep)
			.listener(new BaseJobExecutionListener())
			.build();
	}

	@Bean("heroRankingStep")
	public Step heroRankingStep(HeroRankingReader heroRankingReader, HeroRankingProcessor heroRankingProcessor,
			HeroRankingWriter heroRankingWriter) {
		var itemListener = new BaseItemExecutionListener<Integer, List<OpenDotaHeroRankingDto>>();
		return new StepBuilder("heroRankingStep", jobRepository)
			.<Integer, List<OpenDotaHeroRankingDto>>chunk(chunkSize, transactionManager)
			.reader(heroRankingReader)
			.processor(heroRankingProcessor)
			.writer(heroRankingWriter)
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
