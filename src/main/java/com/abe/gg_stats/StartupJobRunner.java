package com.abe.gg_stats;

import com.abe.gg_stats.service.BatchSchedulerService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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
	public void run(ApplicationArguments args) {
		try (ExecutorService executor = Executors.newFixedThreadPool(5)) {
			// executor.submit(batchSchedulerService::triggerHeroesUpdate);
			// executor.submit(batchSchedulerService::triggerNotablePlayerUpdate);
			// executor.submit(batchSchedulerService::triggerTeamsUpdate);
			executor.submit(batchSchedulerService::triggerHeroRankingUpdate);
			// executor.submit(batchSchedulerService::triggerPlayerUpdate);
		}
	}

}