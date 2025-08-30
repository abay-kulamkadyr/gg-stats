package com.abe.gg_stats.batch.listener;

import org.springframework.stereotype.Component;

/**
 * Job execution listener for the Heroes batch job. Manages MDC context for the entire
 * heroes update job lifecycle.
 */
@Component
public class HeroesJobExecutionListener extends BaseJobExecutionListener {

	// Inherits all functionality from BaseJobExecutionListener
	// Can override methods here if heroes-specific logic is needed

}
