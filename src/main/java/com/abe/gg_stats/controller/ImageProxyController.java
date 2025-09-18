package com.abe.gg_stats.controller;

import com.abe.gg_stats.util.LoggingUtils;
import static net.logstash.logback.argument.StructuredArguments.kv;
import java.io.ByteArrayInputStream;
import java.net.InetAddress;
import java.net.URI;
import java.net.URLConnection;
import java.util.Set;
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

	private static final long MAX_BYTES = 5L * 1024L * 1024L; // 5 MB

	private static final Set<String> ALLOWLIST_HOST_SUFFIXES = Set.of("steamcdn-a.akamaihd.net", "cdn.steamstatic.com",
			"cdn.cloudflare.steamstatic.com");

	private final RestTemplate restTemplate;

	public ImageProxyController(RestTemplate restTemplate) {
		this.restTemplate = restTemplate;
	}

	@GetMapping
	public ResponseEntity<byte[]> proxy(@RequestParam("url") String url) {
		try {
			log.info("Proxy request", kv("url", url));
			URI uri = URI.create(url);
			String scheme = uri.getScheme();
			String host = uri.getHost();
			if (scheme == null || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))
					|| host == null) {
				return ResponseEntity.badRequest().body(new byte[0]);
			}

			// Prevent SSRF: allowlist host suffixes
			String hostLc = host.toLowerCase();
			boolean allowed = ALLOWLIST_HOST_SUFFIXES.stream().anyMatch(hostLc::endsWith);
			if (!allowed) {
				return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new byte[0]);
			}

			// Resolve DNS and block private/loopback/link-local
			for (InetAddress addr : InetAddress.getAllByName(hostLc)) {
				if (addr.isAnyLocalAddress() || addr.isLoopbackAddress() || addr.isSiteLocalAddress()
						|| addr.isLinkLocalAddress() || addr.isMulticastAddress()) {
					return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new byte[0]);
				}
			}

			HttpHeaders headers = new HttpHeaders();
			headers.add(HttpHeaders.USER_AGENT, "Mozilla/5.0 (compatible; gg-stats/1.0)");
			HttpEntity<Void> req = new HttpEntity<>(headers);

			// HEAD check for content type and size
			var headResp = restTemplate.exchange(uri, HttpMethod.HEAD, req, Void.class);
			if (!headResp.getStatusCode().is2xxSuccessful()) {
				return ResponseEntity.status(headResp.getStatusCode()).body(new byte[0]);
			}
			String ct = headResp.getHeaders().getFirst(HttpHeaders.CONTENT_TYPE);
			if (ct != null && !ct.toLowerCase().startsWith("image/")) {
				return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body(new byte[0]);
			}
			String cl = headResp.getHeaders().getFirst(HttpHeaders.CONTENT_LENGTH);
			if (cl != null) {
				try {
					long len = Long.parseLong(cl);
					if (len > MAX_BYTES) {
						return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(new byte[0]);
					}
				}
				catch (NumberFormatException ignore) {
					// continue, we will detect after GET if needed
				}
			}

			var resp = restTemplate.exchange(uri, HttpMethod.GET, req, byte[].class);
			if (resp.getStatusCode() != HttpStatus.OK || resp.getBody() == null || resp.getBody().length == 0) {
				return ResponseEntity.status(resp.getStatusCode()).body(new byte[0]);
			}
			byte[] bytes = resp.getBody();
			if (bytes.length > MAX_BYTES) {
				return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(new byte[0]);
			}
			String detected = URLConnection.guessContentTypeFromStream(new ByteArrayInputStream(bytes));
			String contentType = detected != null ? detected : (ct != null ? ct : MediaType.IMAGE_PNG_VALUE);
			log.debug("/img fetched", kv("url", url), kv("bytes", bytes.length), kv("contentType", contentType));
			return ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_DISPOSITION, "inline")
				.contentType(MediaType.parseMediaType(contentType))
				.body(bytes);
		}
		catch (Exception e) {
			log.warn("/img proxy failed", kv("url", url), e);
			return ResponseEntity.status(502).body(new byte[0]);
		}
	}

}
