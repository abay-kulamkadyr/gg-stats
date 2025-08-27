package com.abe.gg_stats.batch.listener;

import com.abe.gg_stats.util.LoggingUtils;
import com.abe.gg_stats.util.StructuredLoggingContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;

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
		// Set up structured logging context for this batch step
		String jobId = stepExecution.getJobExecution().getJobId().toString();
		String stepName = stepExecution.getStepName();
		
		// Extract correlation ID from job parameters or create new one
		String correlationId = stepExecution.getJobParameters().getString("correlationId");
		if (correlationId != null) {
			// Inherit correlation from job launcher
			StructuredLoggingContext.updateContext(StructuredLoggingContext.CORRELATION_ID, correlationId);
		}
		
		// Set up batch-specific context
		String actualCorrelationId = StructuredLoggingContext.setBatchContext(getStepName(), jobId, stepName);
		
		LoggingUtils.logOperationStart(getStepName() + " batch step", 
			"jobId=" + jobId,
			"stepName=" + stepName,
			"correlationId=" + actualCorrelationId,
			"parameters=" + stepExecution.getJobParameters());
	}

	@Override
	public ExitStatus afterStep(StepExecution stepExecution) {
		try {
			if (stepExecution.getStatus().isUnsuccessful()) {
				LoggingUtils.logOperationFailure(getStepName() + " batch step", "Step execution failed",
						stepExecution.getFailureExceptions().get(0));
				return ExitStatus.FAILED;
			}

			LoggingUtils.logOperationSuccess(getStepName() + " batch step", 
				"stepName=" + stepExecution.getStepName(),
				"read=" + stepExecution.getReadCount(), 
				"write=" + stepExecution.getWriteCount(),
				"skip=" + stepExecution.getSkipCount(), 
				"commit=" + stepExecution.getCommitCount(),
				"rollback=" + stepExecution.getRollbackCount(),
				"correlationId=" + StructuredLoggingContext.getCurrentCorrelationId());

			return ExitStatus.COMPLETED;
		} finally {
			// Clear structured logging context to prevent memory leaks
			StructuredLoggingContext.clearContext();
		}
	}

}
