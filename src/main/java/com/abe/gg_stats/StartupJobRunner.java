package com.abe.gg_stats;

import com.abe.gg_stats.service.BatchSchedulerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "app.startup.jobs.enabled", havingValue = "true", matchIfMissing = true)
public class StartupJobRunner implements ApplicationRunner {

	private final BatchSchedulerService batchSchedulerService;

	@Override
	public void run(ApplicationArguments args) throws Exception {
		// Uncomment the job you want to run immediately on startup

		//batchSchedulerService.triggerHeroesUpdate();
		//batchSchedulerService.triggerNotablePlayerUpdate();
		//batchSchedulerService.triggerTeamsUpdate();
		//batchSchedulerService.triggerHeroRankingUpdate();
		batchSchedulerService.triggerPlayerUpdate();
		log.info("Startup jobs completed");
	}

}