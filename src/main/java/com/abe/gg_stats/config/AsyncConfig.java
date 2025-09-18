package com.abe.gg_stats.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import io.micrometer.context.ContextSnapshot;
import org.springframework.core.task.TaskDecorator;
import org.slf4j.MDC;
import java.util.Map;

@Configuration
@EnableAsync
public class AsyncConfig {

	@Bean(name = "taskExecutor")
	public Executor taskExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(5);
		executor.setMaxPoolSize(10);
		executor.setQueueCapacity(100);
		executor.setThreadNamePrefix("Async-");
		executor.setTaskDecorator(micrometerContextTaskDecorator());
		executor.initialize();
		return executor;
	}

	@Bean
	public TaskDecorator micrometerContextTaskDecorator() {
		return runnable -> {
			ContextSnapshot snapshot = ContextSnapshot.captureAll();
			Map<String, String> parentMdc = MDC.getCopyOfContextMap();
			return () -> {
				try (ContextSnapshot.Scope ignored = snapshot.setThreadLocals()) {
					if (parentMdc != null) {
						MDC.setContextMap(parentMdc);
					}
					runnable.run();
				}
				finally {
					MDC.clear();
				}
			};
		};
	}

}