package com.abe.gg_stats.batch.listener;

import java.time.Duration;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;

@Slf4j
public class BaseStepExecutionListener implements StepExecutionListener {

	@Override
	public void beforeStep(StepExecution stepExecution) {
		log.info("Starting Step: {} for Job: {} (ID: {})", stepExecution.getStepName(),
				stepExecution.getJobExecution().getJobInstance().getJobName(),
				stepExecution.getJobExecution().getJobId());
	}

	@Override
	public ExitStatus afterStep(StepExecution stepExecution) {
		Duration duration = Duration.between(Objects.requireNonNull(stepExecution.getStartTime()),
				stepExecution.getEndTime());
		long durationMillis = duration.toMillis();
		String durationFormatted = String.format("%d min, %d sec, %d ms", duration.toMinutes(),
				duration.minusMinutes(duration.toMinutes()).getSeconds(), duration.toMillisPart());

		double processingRate = (double) stepExecution.getReadCount() / (durationMillis / 1000.0);

		if (stepExecution.getStatus().isUnsuccessful()) {
			log.error("Step FAILED: {} for Job: {} (ID: {}). Status: {}. Exceptions: {}", stepExecution.getStepName(),
					stepExecution.getJobExecution().getJobInstance().getJobName(),
					stepExecution.getJobExecution().getJobId(), stepExecution.getExitStatus(),
					stepExecution.getFailureExceptions());
		}
		else {
			log.info(
					"Step COMPLETED: {} for Job: {} (ID: {}). Summary: read={}, written={}, skipped={}, committed={}, duration={}, rate={} items/sec",
					stepExecution.getStepName(), stepExecution.getJobExecution().getJobInstance().getJobName(),
					stepExecution.getJobExecution().getJobId(), stepExecution.getReadCount(),
					stepExecution.getWriteCount(), stepExecution.getSkipCount(), stepExecution.getCommitCount(),
					durationFormatted, String.format("%.2f", processingRate));
		}

		return stepExecution.getExitStatus();
	}

}
