package com.abe.gg_stats.batch.listener;

import org.springframework.stereotype.Component;

/**
 * Job execution listener for the Notable Players batch job. Manages MDC context for the
 * entire notable players update job lifecycle.
 */
@Component
public class NotablePlayersJobExecutionListener extends BaseJobExecutionListener {

	// Inherits all functionality from BaseJobExecutionListener
	// Can override methods here if notable players-specific logic is needed

}
