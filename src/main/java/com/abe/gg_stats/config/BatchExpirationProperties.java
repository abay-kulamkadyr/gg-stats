package com.abe.gg_stats.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for batch job data expiration periods.
 */
@ConfigurationProperties(prefix = "app.batch.expiration")
public record BatchExpirationProperties(Duration heroes, Duration teams, Duration notableplayers, Duration herorankings,
		Duration players, Duration defaults) {

}
