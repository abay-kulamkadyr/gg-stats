package com.abe.gg_stats;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
@EnableScheduling
@RequiredArgsConstructor
public class BatchScheduler {

  private final JobLauncher jobLauncher;
  private final Job importDotaDataJob;

  @Scheduled(cron = "0 0 3 * * *") // Every day at 3 AM
  public void runJob() throws Exception {
    jobLauncher.run(importDotaDataJob, new JobParametersBuilder()
        .addLong("time", System.currentTimeMillis())
        .toJobParameters());
  }
}