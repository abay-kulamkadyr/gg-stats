package com.abe.gg_stats.batch.listener;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;

import com.abe.gg_stats.util.LoggingUtils;

/**
 * Base class for step execution listeners to reduce code duplication and provide
 * consistent logging using LoggingUtils.
 */
@Slf4j
public abstract class BaseStepExecutionListener implements StepExecutionListener {

	/**
	 * Get the step name for logging purposes
	 */
	protected abstract String getStepName();

	@Override
	public void beforeStep(StepExecution stepExecution) {
		LoggingUtils.logOperationStart(getStepName() + " batch step", "step=" + stepExecution.getStepName(),
				"parameters=" + stepExecution.getJobParameters());
	}

	@Override
	public ExitStatus afterStep(StepExecution stepExecution) {
		if (stepExecution.getStatus().isUnsuccessful()) {
			LoggingUtils.logOperationFailure(getStepName() + " batch step", "Step execution failed",
					stepExecution.getFailureExceptions().get(0));
			return ExitStatus.FAILED;
		}

		LoggingUtils.logOperationSuccess(getStepName() + " batch step", "step=" + stepExecution.getStepName(),
				"read=" + stepExecution.getReadCount(), "write=" + stepExecution.getWriteCount(),
				"skip=" + stepExecution.getSkipCount(), "commit=" + stepExecution.getCommitCount(),
				"rollback=" + stepExecution.getRollbackCount());

		return ExitStatus.COMPLETED;
	}

}
