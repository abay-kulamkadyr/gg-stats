package com.abe.gg_stats.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import io.micrometer.context.ContextRegistry;
import io.micrometer.context.ThreadLocalAccessor;
import org.slf4j.MDC;
import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class WebConfig implements WebMvcConfigurer {

	@PostConstruct
	public void registerMdcAccessor() {
		ContextRegistry.getInstance().registerThreadLocalAccessor(new ThreadLocalAccessor<Map<String, String>>() {
			@Override
			public Object key() {
				return "slf4j.mdc";
			}

			@Override
			public Map<String, String> getValue() {
				Map<String, String> contextMap = MDC.getCopyOfContextMap();
				return contextMap == null ? null : new HashMap<>(contextMap);
			}

			@Override
			public void setValue(Map<String, String> value) {
				if (value == null) {
					MDC.clear();
				}
				else {
					MDC.setContextMap(value);
				}
			}

			@Override
			public void reset() {
				MDC.clear();
			}
		});
	}

	@Bean
	public RestTemplate restTemplate() {
		SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
		factory.setConnectTimeout(10000);
		factory.setReadTimeout(20000);
		return new RestTemplate(factory);
	}

	@Override
	public void addCorsMappings(CorsRegistry registry) {
		registry.addMapping("/api/**")
			.allowedOrigins("*")
			.allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
			.allowedHeaders("*");

		registry.addMapping("/pro/**")
			.allowedOrigins("*")
			.allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
			.allowedHeaders("*");

		registry.addMapping("/img/**").allowedOrigins("*").allowedMethods("GET", "OPTIONS").allowedHeaders("*");
	}

}