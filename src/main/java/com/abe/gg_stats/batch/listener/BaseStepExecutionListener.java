package com.abe.gg_stats.batch.listener;

import com.abe.gg_stats.util.LoggingConstants;
import com.abe.gg_stats.util.LoggingUtils;
import com.abe.gg_stats.util.MDCLoggingContext;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;

/**
 * Base class for step execution listeners to reduce code duplication and provide
 * consistent logging using LoggingUtils.
 */
public abstract class BaseStepExecutionListener implements StepExecutionListener {

	/**
	 * Get the step name for logging purposes
	 */
	protected abstract String getStepName();

	/**
	 * Get the batch type for enhanced context (optional override)
	 */
	protected String getBatchType() {
		return getStepName();
	}

	/**
	 * Calculate processing rate for performance monitoring (optional override)
	 */
	protected double calculateProcessingRate(StepExecution stepExecution) {
		long duration = stepExecution.getEndTime() != null && stepExecution.getStartTime() != null
				? java.time.Duration.between(stepExecution.getStartTime(), stepExecution.getEndTime()).toMillis()
				: 1000; // fallback to 1 second if times not available

		return duration > 0 ? (double) stepExecution.getReadCount() / (duration / 1000.0) : 0.0;
	}

	/**
	 * Get expected item count for progress tracking (optional override)
	 */
	protected long getExpectedItemCount(StepExecution stepExecution) {
		// Default implementation - subclasses can override with specific logic
		return -1; // Unknown
	}

	@Override
	public void beforeStep(StepExecution stepExecution) {
		// Set up structured logging context for this batch step
		String jobId = stepExecution.getJobExecution().getJobId().toString();
		String stepName = stepExecution.getStepName();

		// Extract correlation ID from job parameters
		String correlationId = stepExecution.getJobParameters().getString("correlationId");

		// Check if we already have MDC context (from job execution listener)
		String existingCorrelationId = MDCLoggingContext.getCurrentCorrelationId();

		if (existingCorrelationId != null && existingCorrelationId.equals(correlationId)) {
			// Context already set by job execution listener, just add step-specific info
			MDCLoggingContext.updateContext("stepName", stepName);
			MDCLoggingContext.updateContext("batchType", getBatchType());
			MDCLoggingContext.updateContext("expectedItemCount", String.valueOf(getExpectedItemCount(stepExecution)));
		}
		else if (correlationId != null) {
			// Set up new context if none exists (fallback for standalone step execution)
			MDCLoggingContext.setBatchContext(getStepName(), jobId, stepName);
			MDCLoggingContext.updateContext("correlationId", correlationId);
			MDCLoggingContext.updateContext("batchType", getBatchType());
			MDCLoggingContext.updateContext("expectedItemCount", String.valueOf(getExpectedItemCount(stepExecution)));
		}
		else {
			// If no correlationId in job parameters, try to preserve existing context
			if (existingCorrelationId != null) {
				MDCLoggingContext.updateContext("stepName", stepName);
				MDCLoggingContext.updateContext("batchType", getBatchType());
				MDCLoggingContext.updateContext("expectedItemCount",
						String.valueOf(getExpectedItemCount(stepExecution)));
			}
		}

		LoggingUtils.logOperationStart(getStepName() + " batch step", "stepName=" + stepName,
				"batchType=" + getBatchType(), "expectedItemCount=" + getExpectedItemCount(stepExecution));
	}

	@Override
	public ExitStatus afterStep(StepExecution stepExecution) {
		if (stepExecution.getStatus().isUnsuccessful()) {
			LoggingUtils.logOperationFailure(getStepName() + " batch step", "Step execution failed",
					stepExecution.getFailureExceptions().getFirst());
			return ExitStatus.FAILED;
		}

		double processingRate = calculateProcessingRate(stepExecution);
		long duration = stepExecution.getEndTime() != null && stepExecution.getStartTime() != null
				? java.time.Duration.between(stepExecution.getStartTime(), stepExecution.getEndTime()).toMillis() : 0;

		LoggingUtils.logOperationSuccess(getStepName() + " batch step", "stepName=" + stepExecution.getStepName(),
				"read=" + stepExecution.getReadCount(), "write=" + stepExecution.getWriteCount(),
				"skip=" + stepExecution.getSkipCount(), "commit=" + stepExecution.getCommitCount(),
				"rollback=" + stepExecution.getRollbackCount(), "duration=" + duration + "ms",
				"processingRate=" + String.format("%.2f", processingRate) + " items/sec");

		return ExitStatus.COMPLETED;
		// Removed MDC context clearing - context should persist for the entire job
		// MDC context will be cleared at the job level, not step level
	}

}
