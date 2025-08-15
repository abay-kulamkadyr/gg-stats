package com.abe.gg_stats.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
// @EnableWebSecurity // Temporarily disabled for development
public class SecurityConfig {

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		http.csrf(csrf -> csrf.disable())
			.authorizeHttpRequests(authz -> authz.requestMatchers("/api/**")
				.permitAll()
				.requestMatchers("/actuator/**")
				.permitAll()
				.requestMatchers("/h2-console/**")
				.permitAll()
				.requestMatchers("/", "/index.html", "/static/**", "/css/**", "/js/**", "/images/**")
				.permitAll()
				.anyRequest()
				.authenticated())
			.httpBasic(httpBasic -> {
			});

		// Allow H2 console frames (if using H2 for development)
		http.headers(headers -> headers.frameOptions(frameOptions -> frameOptions.disable()));

		return http.build();
	}

}
