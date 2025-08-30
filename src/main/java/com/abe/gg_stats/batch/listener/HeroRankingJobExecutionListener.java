package com.abe.gg_stats.batch.listener;

import org.springframework.stereotype.Component;

/**
 * Job execution listener for the Hero Ranking batch job. Manages MDC context for the
 * entire hero ranking update job lifecycle.
 */
@Component
public class HeroRankingJobExecutionListener extends BaseJobExecutionListener {

	// Inherits all functionality from BaseJobExecutionListener
	// Can override methods here if hero ranking-specific logic is needed

}
