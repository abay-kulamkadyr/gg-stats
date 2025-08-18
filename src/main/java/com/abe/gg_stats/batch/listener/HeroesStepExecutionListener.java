package com.abe.gg_stats.batch.listener;

import org.springframework.stereotype.Component;

@Component
public class HeroesStepExecutionListener extends BaseStepExecutionListener {

	@Override
	protected String getStepName() {
		return "Heroes";
	}

}
