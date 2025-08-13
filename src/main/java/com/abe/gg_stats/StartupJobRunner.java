package com.abe.gg_stats;

import com.abe.gg_stats.service.BatchSchedulerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class StartupJobRunner implements ApplicationRunner {

	private final BatchSchedulerService batchSchedulerService;

	@Override
	public void run(ApplicationArguments args) throws Exception {
		// Uncomment the job you want to run immediately on startup

		// batchSchedulerService.triggerHeroesUpdate();
		// batchSchedulerService.triggerProPlayersUpdate();
		// batchSchedulerService.triggerTeamsUpdate();
	  // batchSchedulerService.triggerHeroRankingUpdate();

		log.info("Startup jobs completed");
	}

}