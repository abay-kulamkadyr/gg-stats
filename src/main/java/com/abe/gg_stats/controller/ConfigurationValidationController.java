package com.abe.gg_stats.controller;

import com.abe.gg_stats.service.ConfigurationValidationService;
import com.abe.gg_stats.service.ConfigurationValidationService.ConfigurationSummary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/configuration")
class ConfigurationValidationController {

	private final ConfigurationValidationService configService;

	@Autowired
	ConfigurationValidationController(ConfigurationValidationService configService) {
		this.configService = configService;
	}

	@GetMapping
	ConfigurationSummary getConfigurationSummery() {
		return configService.getConfigurationSummary();
	}

}
