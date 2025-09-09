package com.abe.gg_stats.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
@Slf4j
public class TeamLogoService {

    private final RestTemplate restTemplate;

    public String resolveLogoUrl(Long teamId, String existingLogoUrl) {
        if (teamId == null || teamId <= 0) {
            return existingLogoUrl;
        }
        // Prefer Steam CDN if available
        String cdnUrl = "https://steamcdn-a.akamaihd.net/apps/dota2/images/team_logos/" + teamId + ".png";
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.USER_AGENT, "Mozilla/5.0 (compatible; gg-stats/1.0)");
            HttpEntity<Void> req = new HttpEntity<>(headers);
            ResponseEntity<Void> resp = restTemplate.exchange(cdnUrl, HttpMethod.HEAD, req, Void.class);
            if (resp.getStatusCode().is2xxSuccessful()) {
                return cdnUrl;
            }
        } catch (Exception e) {
            log.debug("CDN logo not available for team {}", teamId);
        }
        return existingLogoUrl;
    }
}


