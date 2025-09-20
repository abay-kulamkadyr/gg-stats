package com.abe.gg_stats.exception;

import static net.logstash.logback.argument.StructuredArguments.kv;

import com.abe.gg_stats.dto.response.ImageProxyDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;

@ControllerAdvice
@Slf4j
public class GlobalExceptionWebHandler {

	@ExceptionHandler(ImageProxyException.class)
	public ResponseEntity<ImageProxyDto> handleImageProxyException(ImageProxyException ex) {
		log.warn("Image proxy exception, status={}, message={}", ex.getStatus(), ex.getMessage(), ex);
		ResponseEntity<byte[]> errorResponse = ResponseEntity.status(ex.getStatus()).body(new byte[0]);
		return ResponseEntity.status(ex.getStatus()).body(new ImageProxyDto(errorResponse));
	}

	@ExceptionHandler(HttpClientErrorException.class)
	public ResponseEntity<ImageProxyDto> handleHttpClientError(HttpClientErrorException ex) {
		log.warn("HTTP client error, status={}, message={}", ex.getStatusCode(), ex.getMessage(), ex);
		ResponseEntity<byte[]> errorResponse = ResponseEntity.status(ex.getStatusCode()).body(new byte[0]);
		return ResponseEntity.status(ex.getStatusCode()).body(new ImageProxyDto(errorResponse));
	}

	@ExceptionHandler(ResourceAccessException.class)
	public ResponseEntity<ImageProxyDto> handleResourceAccess(ResourceAccessException ex) {
		log.warn("Resource access error: {}", ex.getMessage(), ex);
		ResponseEntity<byte[]> errorResponse = ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(new byte[0]);
		return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(new ImageProxyDto(errorResponse));
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ImageProxyDto> handleGenericException(Exception ex) {
		log.error("Unexpected error: {}", ex.toString(), ex);
		ResponseEntity<byte[]> errorResponse = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
			.body(new byte[0]);
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ImageProxyDto(errorResponse));
	}

}
