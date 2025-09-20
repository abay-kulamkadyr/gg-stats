package com.abe.gg_stats.config.batch;

import com.abe.gg_stats.batch.listener.BaseItemExecutionListener;
import com.abe.gg_stats.batch.listener.BaseJobExecutionListener;
import com.abe.gg_stats.batch.listener.BaseStepExecutionListener;
import com.abe.gg_stats.batch.player.PlayerReader;
import com.abe.gg_stats.batch.player.PlayerProcessor;
import com.abe.gg_stats.batch.player.PlayerWriter;
import com.abe.gg_stats.dto.request.opendota.OpenDotaPlayerDto;
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
		return new JobBuilder("playerUpdateJob", jobRepository) //
			.incrementer(new RunIdIncrementer())
			.start(playerStep)
			.listener(new BaseJobExecutionListener())
			.build();
	}

	@Bean("playerStep")
	public Step playerStep(PlayerReader playerReader, PlayerProcessor playerProcessor, PlayerWriter playerWriter) {
		var itemListener = new BaseItemExecutionListener<Long, OpenDotaPlayerDto>();
		return new StepBuilder("playerStep", jobRepository)
			.<Long, OpenDotaPlayerDto>chunk(chunkSize, transactionManager)
			.reader(playerReader)
			.processor(playerProcessor)
			.writer(playerWriter)
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
