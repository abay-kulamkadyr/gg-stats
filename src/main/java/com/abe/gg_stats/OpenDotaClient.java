package com.abe.gg_stats;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
public class OpenDotaClient {

  private static final String BASE_URL = "https://api.opendota.com/api";
  private static final int REQUESTS_PER_MINUTE = 60;
  private static final Duration MIN_INTERVAL = Duration.ofMillis(1000 * 60 / REQUESTS_PER_MINUTE);

  private final RestTemplate restTemplate = new RestTemplate();
  private Instant lastRequestTime = Instant.EPOCH;

  private void rateLimit() {
    Instant now = Instant.now();
    Duration sinceLast = Duration.between(lastRequestTime, now);
    if (sinceLast.compareTo(MIN_INTERVAL) < 0) {
      try {
        Thread.sleep(MIN_INTERVAL.minus(sinceLast).toMillis());
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }
    lastRequestTime = Instant.now();
  }

  public List<Map<String, Object>> getHeroes() {
    rateLimit();
    ResponseEntity<List<Map<String, Object>>> response =
        restTemplate.exchange(BASE_URL + "/heroes", HttpMethod.GET, null,
            new ParameterizedTypeReference<>() {});
    return response.getBody();
  }

  public List<Map<String, Object>> getProMatches() {
    rateLimit();
    ResponseEntity<List<Map<String, Object>>> response =
        restTemplate.exchange(BASE_URL + "/proMatches", HttpMethod.GET, null,
            new ParameterizedTypeReference<>() {});
    return response.getBody();
  }
}
