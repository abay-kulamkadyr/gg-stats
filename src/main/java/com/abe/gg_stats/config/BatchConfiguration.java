package com.abe.gg_stats.config;

import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.support.TaskExecutorJobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.support.JobRepositoryFactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;

/**
 * Main batch configuration class. Individual batch job configurations are now separated
 * into dedicated classes: - HeroesBatchConfig - PlayersBatchConfig - TeamsBatchConfig -
 * NotablePlayersBatchConfig - HeroRankingsBatchConfig
 * <p>
 * This separation improves maintainability and follows single responsibility principle.
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class BatchConfiguration {

	@Autowired
	private DataSource dataSource;

	@Bean
	public JobRepository jobRepository() throws Exception {
		JobRepositoryFactoryBean factory = new JobRepositoryFactoryBean();
		factory.setDataSource(dataSource);
		factory.setTransactionManager(new DataSourceTransactionManager(dataSource));
		factory.setIsolationLevelForCreate("ISOLATION_READ_COMMITTED"); // Or another
																		// suitable level
		factory.setDatabaseType("POSTGRES"); // Set this to your database type
		factory.setTablePrefix("BATCH_");
		factory.afterPropertiesSet();
		return factory.getObject();
	}

	@Bean
	public JobLauncher asyncJobLauncher() throws Exception {
		TaskExecutorJobLauncher jobLauncher = new TaskExecutorJobLauncher();
		jobLauncher.setJobRepository(jobRepository());
		jobLauncher.setTaskExecutor(new SimpleAsyncTaskExecutor());
		jobLauncher.afterPropertiesSet();
		return jobLauncher;
	}

}