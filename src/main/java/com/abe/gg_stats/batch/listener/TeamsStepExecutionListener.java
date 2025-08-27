package com.abe.gg_stats.batch.listener;

import org.springframework.stereotype.Component;

/**
 * Step execution listener for Teams batch operations.
 * Provides consistent logging using the BaseStepExecutionListener pattern.
 */
@Component
public class TeamsStepExecutionListener extends BaseStepExecutionListener {

	@Override
	protected String getStepName() {
		return "Teams";
	}

}
