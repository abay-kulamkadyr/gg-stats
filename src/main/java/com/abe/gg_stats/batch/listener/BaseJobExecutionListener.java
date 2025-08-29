package com.abe.gg_stats.batch.listener;

import com.abe.gg_stats.util.LoggingConstants;
import com.abe.gg_stats.util.LoggingUtils;
import com.abe.gg_stats.util.MDCLoggingContext;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.StepExecution;

/**
 * Base class for job execution listeners to manage MDC context for the entire job lifecycle.
 * This ensures that MDC context persists across all steps and is only cleared when the job completes.
 */
public abstract class BaseJobExecutionListener implements JobExecutionListener {

      @Override
  public void beforeJob(JobExecution jobExecution) {
    // Extract correlation ID from job parameters or create new one
    String correlationId = jobExecution.getJobParameters().getString("correlationId");
    
    // Set up job-level context that will persist across all steps
    if (correlationId != null) {
      MDCLoggingContext.setBatchContext(
        jobExecution.getJobInstance().getJobName(),
        jobExecution.getJobId().toString(),
        null // stepName is null for job-level context
      );
      
      // Update with additional job-specific context
      MDCLoggingContext.updateContext("jobId", jobExecution.getJobId().toString());
      MDCLoggingContext.updateContext("jobName", jobExecution.getJobInstance().getJobName());
      MDCLoggingContext.updateContext("correlationId", correlationId);
      MDCLoggingContext.updateContext("operationType", LoggingConstants.OPERATION_TYPE_BATCH);
    }
    
    LoggingUtils.logOperationStart("Starting batch job", 
      "jobName=" + jobExecution.getJobInstance().getJobName(),
      "jobId=" + jobExecution.getJobId(),
      "correlationId=" + correlationId);
  }

      @Override
  public void afterJob(JobExecution jobExecution) {
    try {
      if (jobExecution.getStatus().isUnsuccessful()) {
        LoggingUtils.logOperationFailure("Batch job execution", 
          "Job failed",
          new RuntimeException("Job execution failed with status: " + jobExecution.getStatus()),
          "jobName=" + jobExecution.getJobInstance().getJobName(),
          "jobId=" + jobExecution.getJobId(),
          "correlationId=" + MDCLoggingContext.getCurrentCorrelationId(),
          "exitStatus=" + jobExecution.getExitStatus(),
          "failureExceptions=" + jobExecution.getFailureExceptions());
      } else {
        // Calculate totals from all step executions
        long totalReadCount = 0;
        long totalWriteCount = 0;

        jobExecution.getStepExecutions();
        totalReadCount = jobExecution.getStepExecutions().stream()
          .mapToLong(StepExecution::getReadCount)
          .sum();
        totalWriteCount = jobExecution.getStepExecutions().stream()
          .mapToLong(StepExecution::getWriteCount)
          .sum();

        LoggingUtils.logOperationSuccess("Batch job execution", 
          "jobName=" + jobExecution.getJobInstance().getJobName(),
          "jobId=" + jobExecution.getJobId(),
          "correlationId=" + MDCLoggingContext.getCurrentCorrelationId(),
          "exitStatus=" + jobExecution.getExitStatus(),
          "readCount=" + totalReadCount,
          "writeCount=" + totalWriteCount);
      }
    } finally {
      // Only clear context after the entire job completes
      // This ensures all steps within the job maintain the same context
      MDCLoggingContext.clearContext();
      LoggingUtils.logDebug("Cleared MDC context for job", 
        "jobName=" + jobExecution.getJobInstance().getJobName(),
        "jobId=" + jobExecution.getJobId());
    }
  }
}
