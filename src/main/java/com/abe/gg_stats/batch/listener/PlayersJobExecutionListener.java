package com.abe.gg_stats.batch.listener;

import org.springframework.stereotype.Component;

/**
 * Job execution listener for the Players batch job.
 * Manages MDC context for the entire players update job lifecycle.
 */
@Component
public class PlayersJobExecutionListener extends BaseJobExecutionListener {
    // Inherits all functionality from BaseJobExecutionListener
    // Can override methods here if players-specific logic is needed
}
