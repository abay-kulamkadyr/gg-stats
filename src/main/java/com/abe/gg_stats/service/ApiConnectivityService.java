package com.abe.gg_stats.service;

import com.abe.gg_stats.dto.response.ActionResponse;
import io.micrometer.core.annotation.Timed;
import java.time.Instant;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class ApiConnectivityService {

	private final OpenDotaApiService openDotaApiService;

	private final ServiceLogger serviceLogger;

	public ApiConnectivityService(OpenDotaApiService openDotaApiService, ServiceLogger serviceLogger) {
		this.openDotaApiService = openDotaApiService;
		this.serviceLogger = serviceLogger;
	}

	@Timed(description = "Test API connectivity")
	public ResponseEntity<ActionResponse> testApiConnectivity() {
		serviceLogger.logServiceStart("ApiConnectivityService", "Testing API connectivity");
		try {
			var result = openDotaApiService.getHeroes();
			if (result.isPresent()) {
				ActionResponse response = ActionResponse.success("API connectivity test successful", Map.of("testTime",
						Instant.now().toString(), "responseReceived", true, "heroesCount", result.get().size()));
				serviceLogger.logServiceSuccess("ApiConnectivityService", "API connectivity test passed");
				return ResponseEntity.ok(response);
			}
			else {
				ActionResponse response = ActionResponse.error("API connectivity test failed - no response received");
				serviceLogger.logServiceFailure("api_connectivity_test", "No response received from API", null);
				return ResponseEntity.status(503).body(response);
			}
		}
		catch (Exception e) {
			serviceLogger.logServiceFailure("api_connectivity_test", "API connectivity test failed", e);
			ActionResponse response = ActionResponse.error("API connectivity test failed: " + e.getMessage());
			return ResponseEntity.status(503).body(response);
		}
	}

}
