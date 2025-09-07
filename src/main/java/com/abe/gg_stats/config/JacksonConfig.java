package com.abe.gg_stats.config;

import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.deser.DeserializationProblemHandler;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonConfig {

	@Bean
	public ObjectMapper objectMapper() {
		ObjectMapper mapper = new ObjectMapper();
		mapper.registerModule(new JavaTimeModule());
		mapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		// Add custom problem handler
		mapper.addHandler(new DeserializationProblemHandler() {
			@Override
			public Object handleWeirdStringValue(DeserializationContext ctxt, Class<?> targetType,
					String valueToConvert, String failureMsg) {
				// e.g., String "abc" for a Long field → return null
				return null;
			}

			@Override
			public Object handleWeirdNumberValue(DeserializationContext ctxt, Class<?> targetType,
					Number valueToConvert, String failureMsg) {
				// e.g., Number too big/small → return null
				return null;
			}
		});

		return mapper;
	}

}


