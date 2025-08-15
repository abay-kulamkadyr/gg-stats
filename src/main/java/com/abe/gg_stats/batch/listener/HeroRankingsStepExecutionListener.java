package com.abe.gg_stats.batch.listener;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class HeroRankingsStepExecutionListener implements StepExecutionListener {

	@Override
	public void beforeStep(StepExecution stepExecution) {
		log.info("Starting Hero Rankings batch step: {}", stepExecution.getStepName());
		log.info("Step parameters: {}", stepExecution.getJobParameters());
	}

	@Override
	public ExitStatus afterStep(StepExecution stepExecution) {
		log.info("Hero Rankings batch step completed: {}", stepExecution.getStepName());
		log.info("Read count: {}", stepExecution.getReadCount());
		log.info("Write count: {}", stepExecution.getWriteCount());
		log.info("Skip count: {}", stepExecution.getSkipCount());
		log.info("Commit count: {}", stepExecution.getCommitCount());
		log.info("Rollback count: {}", stepExecution.getRollbackCount());

		if (stepExecution.getStatus().isUnsuccessful()) {
			log.error("Hero Rankings batch step failed: {}", stepExecution.getFailureExceptions());
			return ExitStatus.FAILED;
		}

		return ExitStatus.COMPLETED;
	}

}
