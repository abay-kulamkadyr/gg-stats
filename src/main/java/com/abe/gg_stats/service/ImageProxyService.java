package com.abe.gg_stats.service;

import com.abe.gg_stats.dto.response.ImageProxyDto;
import com.abe.gg_stats.exception.ImageProxyException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class ImageProxyService {

	private static final long MAX_BYTES = 5L * 1024L * 1024L; // 5 MB

	@Value("${app.allowlist.host-suffixes}")
	private Set<String> allowlistHostSuffixes;

	private final RestTemplate restTemplate;

	public ImageProxyService(RestTemplate restTemplate) {
		this.restTemplate = restTemplate;
	}

	public ImageProxyDto proxyImage(String url) {
		URI uri = URI.create(url);
		String scheme = uri.getScheme();
		String host = uri.getHost();

		if (scheme == null || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https")) || host == null) {
			throw new ImageProxyException("Invalid URL schema or host.", HttpStatus.BAD_REQUEST);
		}

		// --- Allowlist check ---
		String hostLc = host.toLowerCase();
		if (allowlistHostSuffixes.stream().noneMatch(hostLc::endsWith)) {
			throw new ImageProxyException("Host not in allowlist.", HttpStatus.FORBIDDEN);
		}

		// --- DNS check ---
		validateDns(hostLc);

		HttpHeaders headers = new HttpHeaders();
		headers.add(HttpHeaders.USER_AGENT, "Mozilla/5.0 (compatible; gg-stats/1.0)");
		HttpEntity<Void> req = new HttpEntity<>(headers);

		// --- HEAD ---
		ResponseEntity<Void> headResp = restTemplate.exchange(uri, HttpMethod.HEAD, req, Void.class);
		validateHead(headResp);

		// --- GET ---
		ResponseEntity<byte[]> resp = restTemplate.exchange(uri, HttpMethod.GET, req, byte[].class);
		return buildImageProxyDto(resp, headResp.getHeaders().getFirst(HttpHeaders.CONTENT_TYPE));
	}

	private void validateDns(String host) {
		try {
			for (InetAddress addr : InetAddress.getAllByName(host)) {
				if (addr.isAnyLocalAddress() || addr.isLoopbackAddress() || addr.isSiteLocalAddress()
						|| addr.isLinkLocalAddress() || addr.isMulticastAddress()) {
					throw new ImageProxyException("Private IP addresses are not allowed.", HttpStatus.FORBIDDEN);
				}
			}
		}
		catch (UnknownHostException e) {
			throw new ImageProxyException("Could not resolve host.", HttpStatus.BAD_REQUEST, e);
		}
	}

	private void validateHead(ResponseEntity<Void> headResp) {
		if (!headResp.getStatusCode().is2xxSuccessful()) {
			throw new ImageProxyException("HEAD request failed.", (HttpStatus) headResp.getStatusCode());
		}

		String contentType = headResp.getHeaders().getFirst(HttpHeaders.CONTENT_TYPE);
		if (contentType != null && !contentType.toLowerCase().startsWith("image/")) {
			throw new ImageProxyException("Content is not an image.", HttpStatus.UNSUPPORTED_MEDIA_TYPE);
		}

		String contentLengthHeader = headResp.getHeaders().getFirst(HttpHeaders.CONTENT_LENGTH);
		if (contentLengthHeader != null) {
			try {
				long len = Long.parseLong(contentLengthHeader);
				if (len > MAX_BYTES) {
					throw new ImageProxyException("Image payload too large.", HttpStatus.PAYLOAD_TOO_LARGE);
				}
			}
			catch (NumberFormatException ignored) {
				// ignore, size will be validated after GET
			}
		}
	}

	private ImageProxyDto buildImageProxyDto(ResponseEntity<byte[]> resp, String headContentType) {
		if (resp.getStatusCode() != HttpStatus.OK || resp.getBody() == null || resp.getBody().length == 0) {
			throw new ImageProxyException("Failed to retrieve image body.", (HttpStatus) resp.getStatusCode());
		}

		byte[] bytes = resp.getBody();
		if (bytes.length > MAX_BYTES) {
			throw new ImageProxyException("Image payload too large.", HttpStatus.PAYLOAD_TOO_LARGE);
		}

		String detectedType;
		try {
			detectedType = URLConnection.guessContentTypeFromStream(new ByteArrayInputStream(bytes));
		}
		catch (IOException e) {
			throw new ImageProxyException("Content type is not detected", HttpStatus.UNSUPPORTED_MEDIA_TYPE, e);
		}

		String finalContentType = detectedType != null ? detectedType
				: (headContentType != null ? headContentType : MediaType.IMAGE_PNG_VALUE);

		return new ImageProxyDto(ResponseEntity.ok()
			.header(HttpHeaders.CONTENT_DISPOSITION, "inline")
			.contentType(MediaType.parseMediaType(finalContentType))
			.body(bytes));
	}

}
