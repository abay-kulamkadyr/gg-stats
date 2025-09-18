package com.abe.gg_stats.batch.listener;

import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.StepExecution;

@Slf4j
public class BaseJobExecutionListener implements JobExecutionListener {

	@Override
	public void beforeJob(JobExecution jobExecution) {
		log.info("Starting Job: {} (ID: {})", jobExecution.getJobInstance().getJobName(), jobExecution.getJobId());
		jobExecution.getExecutionContext().put("jobStartTime", System.currentTimeMillis());
	}

	@Override
	public void afterJob(JobExecution jobExecution) {
		long durationMillis = System.currentTimeMillis() - jobExecution.getExecutionContext().getLong("jobStartTime");
		String duration = String.format("%d min, %d sec", TimeUnit.MILLISECONDS.toMinutes(durationMillis),
				TimeUnit.MILLISECONDS.toSeconds(durationMillis)
						- TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(durationMillis)));

		if (jobExecution.getStatus().isUnsuccessful()) {
			log.error("Job FAILED: {} (ID: {}). Exit Status: {}. Total duration: {}. Exceptions: {}",
					jobExecution.getJobInstance().getJobName(), jobExecution.getJobId(), jobExecution.getExitStatus(),
					duration, jobExecution.getFailureExceptions());
		}
		else {
			long totalReadCount = jobExecution.getStepExecutions()
				.stream()
				.mapToLong(StepExecution::getReadCount)
				.sum();
			long totalWriteCount = jobExecution.getStepExecutions()
				.stream()
				.mapToLong(StepExecution::getWriteCount)
				.sum();
			long totalSkipCount = jobExecution.getStepExecutions()
				.stream()
				.mapToLong(StepExecution::getSkipCount)
				.sum();
			long totalCommitCount = jobExecution.getStepExecutions()
				.stream()
				.mapToLong(StepExecution::getCommitCount)
				.sum();

			log.info(
					"Job COMPLETED: {} (ID: {}). Exit Status: {}. Total duration: {}. Summary: read={}, written={}, skipped={}, committed={}",
					jobExecution.getJobInstance().getJobName(), jobExecution.getJobId(), jobExecution.getExitStatus(),
					duration, totalReadCount, totalWriteCount, totalSkipCount, totalCommitCount);
		}
	}

}
