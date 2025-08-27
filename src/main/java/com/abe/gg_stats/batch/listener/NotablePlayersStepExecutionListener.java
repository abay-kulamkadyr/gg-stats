package com.abe.gg_stats.batch.listener;

import org.springframework.stereotype.Component;

/**
 * Step execution listener for Notable Players batch operations.
 * Provides consistent logging using the BaseStepExecutionListener pattern.
 */
@Component
public class NotablePlayersStepExecutionListener extends BaseStepExecutionListener {

	@Override
	protected String getStepName() {
		return "Notable Players";
	}

}
