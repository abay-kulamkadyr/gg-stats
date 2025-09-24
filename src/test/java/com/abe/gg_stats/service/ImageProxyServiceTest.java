package com.abe.gg_stats.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.abe.gg_stats.dto.response.ImageProxyDto;
import com.abe.gg_stats.exception.ImageProxyException;
import java.lang.reflect.Field;
import java.net.URI;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

class ImageProxyServiceTest {

	private RestTemplate restTemplate;

	private ImageProxyService service;

	@BeforeEach
	void setUp() throws NoSuchFieldException, IllegalAccessException {
		restTemplate = mock(RestTemplate.class);
		service = new ImageProxyService(restTemplate);
		// manually inject allowlist into private field
		Field field = ImageProxyService.class.getDeclaredField("allowlistHostSuffixes");
		field.setAccessible(true);
		field.set(service, Set.of("steamcdn-a.akamaihd.net", "cdn.steamstatic.com", "cdn.cloudflare.steamstatic.com"));
	}

	@Test
	void rejectsInvalidSchemeOrHost() {
		assertThrows(ImageProxyException.class, () -> service.proxyImage("ftp://example.com/a.png"));
		assertThrows(ImageProxyException.class, () -> service.proxyImage("http:///nohost"));
	}

	@Test
	void rejectsHostNotInAllowlist() {
		assertThrows(ImageProxyException.class, () -> service.proxyImage("https://example.com/a.png"));
	}

	@Test
	void returnsImageWhenHeadAndGetSucceed() {
		String url = "https://cdn.steamstatic.com/some/path/img.png";
		URI uri = URI.create(url);

		// HEAD response OK with image content-type
		ResponseEntity<Void> head = ResponseEntity.ok()
			.header(HttpHeaders.CONTENT_TYPE, MediaType.IMAGE_PNG_VALUE)
			.build();
		when(restTemplate.exchange(eq(uri), eq(HttpMethod.HEAD), any(), eq(Void.class))).thenReturn(head);

		// GET response with body
		byte[] body = new byte[] { (byte) 137, 80, 78, 71 }; // PNG signature start
		ResponseEntity<byte[]> get = ResponseEntity.ok().body(body);
		when(restTemplate.exchange(eq(uri), eq(HttpMethod.GET), any(), eq(byte[].class))).thenReturn(get);

		ImageProxyDto dto = service.proxyImage(url);
		assertNotNull(dto);
		assertEquals(200, dto.responseEntity().getStatusCode().value());
	}

	@Test
	void rejectsWhenHeadNot2xx() {
		String url = "https://cdn.steamstatic.com/some/path/img.png";
		URI uri = URI.create(url);
		ResponseEntity<Void> head = ResponseEntity.status(HttpStatus.NOT_FOUND).build();
		when(restTemplate.exchange(eq(uri), eq(HttpMethod.HEAD), any(), eq(Void.class))).thenReturn(head);
		assertThrows(ImageProxyException.class, () -> service.proxyImage(url));
	}

	@Test
	void rejectsWhenPayloadTooLarge() {
		String url = "https://cdn.steamstatic.com/large.png";
		URI uri = URI.create(url);
		ResponseEntity<Void> head = ResponseEntity.ok()
			.header(HttpHeaders.CONTENT_TYPE, MediaType.IMAGE_PNG_VALUE)
			.header(HttpHeaders.CONTENT_LENGTH, String.valueOf(6L * 1024L * 1024L)) // 6MB
			.build();
		when(restTemplate.exchange(eq(uri), eq(HttpMethod.HEAD), any(), eq(Void.class))).thenReturn(head);
		assertThrows(ImageProxyException.class, () -> service.proxyImage(url));
	}

}
