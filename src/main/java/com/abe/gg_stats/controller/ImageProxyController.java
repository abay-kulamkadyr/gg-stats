package com.abe.gg_stats.controller;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.URLConnection;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("/img")
@Slf4j
public class ImageProxyController {

	private final RestTemplate restTemplate;

	public ImageProxyController(RestTemplate restTemplate) {
		this.restTemplate = restTemplate;
	}

	@GetMapping
	public ResponseEntity<byte[]> proxy(@RequestParam("url") String url) {
		try {
			URI uri = URI.create(url);
			String scheme = uri.getScheme();
			if (scheme == null || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))) {
				return ResponseEntity.badRequest().body(new byte[0]);
			}
			HttpHeaders headers = new HttpHeaders();
			headers.add(HttpHeaders.USER_AGENT, "Mozilla/5.0 (compatible; gg-stats/1.0)");
			HttpEntity<Void> req = new HttpEntity<>(headers);
			var resp = restTemplate.exchange(uri, HttpMethod.GET, req, byte[].class);
			if (resp.getStatusCode() != HttpStatus.OK || resp.getBody() == null || resp.getBody().length == 0) {
				return ResponseEntity.status(resp.getStatusCode()).body(new byte[0]);
			}
			byte[] bytes = resp.getBody();
			String detected = URLConnection.guessContentTypeFromStream(new ByteArrayInputStream(bytes));
			String contentType = detected != null ? detected : MediaType.IMAGE_PNG_VALUE;
			log.debug("/img fetched url={} bytes={} contentType={}", url, bytes.length, contentType);
			return ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_DISPOSITION, "inline")
				.contentType(MediaType.parseMediaType(contentType))
				.body(bytes);
		}
		catch (Exception e) {
			log.warn("/img proxy failed for url={}", url, e);
			return ResponseEntity.status(502).body(new byte[0]);
		}
	}

}
