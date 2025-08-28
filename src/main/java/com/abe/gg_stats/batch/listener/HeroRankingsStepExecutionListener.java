package com.abe.gg_stats.batch.listener;

import org.springframework.stereotype.Component;

/**
 * Step execution listener for Hero Rankings batch operations. Provides consistent logging
 * using the BaseStepExecutionListener pattern.
 */
@Component
public class HeroRankingsStepExecutionListener extends BaseStepExecutionListener {

	@Override
	protected String getStepName() {
		return "Hero Rankings";
	}

}
