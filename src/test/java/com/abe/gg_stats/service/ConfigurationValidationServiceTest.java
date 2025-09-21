package com.abe.gg_stats.service;

import com.abe.gg_stats.exception.ConfigurationException;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ConfigurationValidationServiceTest {

	private ConfigurationValidationService buildService(String baseUrl, long readTimeout, long connectTimeout,
			int perMinute, int perDay, int burst, boolean enableWaiting, int cbFailure, int cbSuccess, long cbTimeout,
			int minCalls, int windowSize, boolean healthEnabled, String activeProfile) {
		ConfigurationValidationService service = new ConfigurationValidationService();
		ReflectionTestUtils.setField(service, "openDotaBaseUrl", baseUrl);
		ReflectionTestUtils.setField(service, "readTimeoutMs", readTimeout);
		ReflectionTestUtils.setField(service, "connectTimeoutMs", connectTimeout);
		ReflectionTestUtils.setField(service, "requestsPerMinute", perMinute);
		ReflectionTestUtils.setField(service, "requestsPerDay", perDay);
		ReflectionTestUtils.setField(service, "burstCapacity", burst);
		ReflectionTestUtils.setField(service, "enableWaiting", enableWaiting);
		ReflectionTestUtils.setField(service, "circuitBreakerFailureThreshold", cbFailure);
		ReflectionTestUtils.setField(service, "circuitBreakerSuccessThreshold", cbSuccess);
		ReflectionTestUtils.setField(service, "circuitBreakerTimeoutMs", cbTimeout);
		ReflectionTestUtils.setField(service, "minimumCalls", minCalls);
		ReflectionTestUtils.setField(service, "slidingWindowSize", windowSize);
		ReflectionTestUtils.setField(service, "healthCheckEnabled", healthEnabled);
		ReflectionTestUtils.setField(service, "activeProfile", activeProfile);
		return service;
	}

	@Test
	void validConfigurationDoesNotThrow() {
		ConfigurationValidationService service = buildService("https://api.opendota.com/api", 5000, 1000, 50, 1000, 10,
				true, 5, 2, 2000, 5, 10, true, "test");

		assertDoesNotThrow(service::validateConfiguration);
	}

	@Test
	void invalidUrlCausesException() {
		ConfigurationValidationService service = buildService("htt p://invalid-url", 5000, 1000, 50, 1000, 10, true, 5,
				2, 2000, 5, 10, true, "test");

		assertThrows(ConfigurationException.class, service::validateConfiguration);
	}

}
