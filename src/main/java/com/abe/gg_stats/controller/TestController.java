package com.abe.gg_stats.controller;

import com.abe.gg_stats.dto.response.ActionResponse;
import com.abe.gg_stats.service.ApiConnectivityService;
import io.micrometer.core.annotation.Timed;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/monitoring/test")
public class TestController {

	private final ApiConnectivityService apiConnectivityService;

	public TestController(ApiConnectivityService apiConnectivityService) {
		this.apiConnectivityService = apiConnectivityService;
	}

	@GetMapping("/api-connectivity")
	@Timed(description = "Test API connectivity")
	public ResponseEntity<ActionResponse> testApiConnectivity() {
		return apiConnectivityService.testApiConnectivity();
	}
}


