package com.abe.gg_stats.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

/**
 * Main batch configuration class. Individual batch job configurations are now separated
 * into dedicated classes: - HeroesBatchConfig - PlayersBatchConfig - TeamsBatchConfig -
 * NotablePlayersBatchConfig - HeroRankingsBatchConfig
 *
 * This separation improves maintainability and follows single responsibility principle.
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class BatchConfiguration {

	// Common batch configuration can be added here if needed
	// Individual job configurations are now in separate classes

}