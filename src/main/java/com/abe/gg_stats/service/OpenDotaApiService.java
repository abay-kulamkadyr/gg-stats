package com.abe.gg_stats.service;

import com.abe.gg_stats.entity.ApiRateLimit;
import com.abe.gg_stats.repository.ApiRateLimitRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class OpenDotaApiService {

	private final RestTemplate restTemplate;

	private final ApiRateLimitRepository rateLimitRepository;

	private final ObjectMapper objectMapper;

	@Value("${opendota.api.base-url:https://api.opendota.com/api}")
	private String baseUrl;

	@Value("${opendota.api.rate-limit.per-minute:50}")
	private int requestsPerMinute;

	@Value("${opendota.api.rate-limit.per-day:1800}")
	private int requestsPerDay;

	/**
	 * Makes a rate-limited API call to OpenDota
	 */
	public Optional<JsonNode> makeApiCall(String endpoint) {
		if (!canMakeRequest(endpoint)) {
			log.warn("Rate limit exceeded for endpoint: {}", endpoint);
			return Optional.empty();
		}

		try {
			String url = baseUrl + endpoint;
			log.debug("Making API call to: {}", url);

			ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

			if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
				updateRateLimit(endpoint);
				return Optional.of(objectMapper.readTree(response.getBody()));
			}

		}
		catch (HttpClientErrorException e) {
			if (e.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
				log.warn("Rate limited by OpenDota API for endpoint: {}", endpoint);
				// Update our rate limit to prevent further requests
				forceUpdateRateLimit(endpoint);
			}
			else {
				log.error("HTTP error calling OpenDota API: {} - {}", e.getStatusCode(), e.getMessage());
			}
		}
		catch (Exception e) {
			log.error("Error calling OpenDota API: {}", e.getMessage(), e);
		}

		return Optional.empty();
	}

	/**
	 * Checks if we can make a request based on rate limits
	 */
	private boolean canMakeRequest(String endpoint) {
		LocalDateTime now = LocalDateTime.now();
		LocalDate today = LocalDate.now();

		// Check daily limit first
		Integer totalDailyRequests = rateLimitRepository.getTotalDailyRequests(today);
		if (totalDailyRequests != null && totalDailyRequests >= requestsPerDay) {
			log.warn("Daily rate limit of {} requests exceeded", requestsPerDay);
			return false;
		}

		// Check per-minute limit for this endpoint
		Optional<ApiRateLimit> rateLimitOpt = rateLimitRepository.findByEndpoint(endpoint);

		if (rateLimitOpt.isPresent()) {
			ApiRateLimit rateLimit = rateLimitOpt.get();

			// Check if we need to reset the minute window
			if (ChronoUnit.MINUTES.between(rateLimit.getWindowStart(), now) >= 1) {
				rateLimit.setRequestsCount(0);
				rateLimit.setWindowStart(now);
			}

			// Check if we need to reset the daily window
			if (!rateLimit.getDailyWindowStart().equals(today)) {
				rateLimit.setDailyRequests(0);
				rateLimit.setDailyWindowStart(today);
			}

			// Check if we can make the request
			return rateLimit.getRequestsCount() < requestsPerMinute;
		}

		return true; // First request for this endpoint
	}

	/**
	 * Updates rate limit counters after a successful request
	 */
	private void updateRateLimit(String endpoint) {
		LocalDateTime now = LocalDateTime.now();
		LocalDate today = LocalDate.now();

		ApiRateLimit rateLimit = rateLimitRepository.findByEndpoint(endpoint)
			.orElse(new ApiRateLimit(null, endpoint, 0, now, 0, today));

		// Update minute window
		if (ChronoUnit.MINUTES.between(rateLimit.getWindowStart(), now) >= 1) {
			rateLimit.setRequestsCount(1);
			rateLimit.setWindowStart(now);
		}
		else {
			rateLimit.setRequestsCount(rateLimit.getRequestsCount() + 1);
		}

		// Update daily window
		if (!rateLimit.getDailyWindowStart().equals(today)) {
			rateLimit.setDailyRequests(1);
			rateLimit.setDailyWindowStart(today);
		}
		else {
			rateLimit.setDailyRequests(rateLimit.getDailyRequests() + 1);
		}

		rateLimitRepository.save(rateLimit);
		log.debug("Updated rate limit for {}: {}/min, {}/day", endpoint, rateLimit.getRequestsCount(),
				rateLimit.getDailyRequests());
	}

	/**
	 * Forces rate limit update when we get a 429 response
	 */
	private void forceUpdateRateLimit(String endpoint) {
		ApiRateLimit rateLimit = rateLimitRepository.findByEndpoint(endpoint)
			.orElse(new ApiRateLimit(null, endpoint, requestsPerMinute, LocalDateTime.now(), 0, LocalDate.now()));

		rateLimit.setRequestsCount(requestsPerMinute); // Max out the minute limit
		rateLimitRepository.save(rateLimit);
	}

	/**
	 * Gets remaining requests for today
	 */
	public int getRemainingDailyRequests() {
		Integer used = rateLimitRepository.getTotalDailyRequests(LocalDate.now());
		return requestsPerDay - (used != null ? used : 0);
	}

	/**
	 * Specific API methods
	 */

	public Optional<JsonNode> getHeroes() {
		return makeApiCall("/heroes");
	}

	public Optional<JsonNode> getProPlayers() {
		return makeApiCall("/proPlayers");
	}

	public Optional<JsonNode> getTeams() {
		return makeApiCall("/teams");
	}

	public Optional<JsonNode> getPlayer(Long accountId) {
		return makeApiCall("/players/" + accountId);
	}

	public Optional<JsonNode> getLeaderboard(String region) {
		return makeApiCall("/leaderboards?leaderboard=" + region);
	}

	public Optional<JsonNode> getPlayerRanking(Long accountId) {
		return makeApiCall("/players/" + accountId + "/rankings");
	}

	public Optional<JsonNode> getPlayerWinLoss(Long accountId) {
		return makeApiCall("/players/" + accountId + "/wl");
	}

	public Optional<JsonNode> getPlayerRecentMatches(Long accountId) {
		return makeApiCall("/players/" + accountId + "/recentMatches");
	}

}