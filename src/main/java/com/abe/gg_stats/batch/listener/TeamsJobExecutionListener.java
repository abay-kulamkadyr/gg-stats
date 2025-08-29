package com.abe.gg_stats.batch.listener;

import org.springframework.stereotype.Component;

/**
 * Job execution listener for the Teams batch job.
 * Manages MDC context for the entire teams update job lifecycle.
 */
@Component
public class TeamsJobExecutionListener extends BaseJobExecutionListener {
    // Inherits all functionality from BaseJobExecutionListener
    // Can override methods here if teams-specific logic is needed
}
