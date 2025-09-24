package com.abe.gg_stats.exception;

import com.abe.gg_stats.dto.response.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionWebHandler {

	private String getRequestPath(HttpServletRequest request) {
		return request.getRequestURI();
	}

	@ExceptionHandler(ImageProxyException.class)
	public ResponseEntity<ApiErrorResponse> handleImageProxyException(ImageProxyException ex,
			HttpServletRequest request) {
		log.warn("Image proxy exception: status={}, message={}", ex.getStatus(), ex.getMessage(), ex);
		return ResponseEntity.status(ex.getStatus())
			.body(ApiErrorResponse.of(ex.getStatus(), ex.getMessage(), getRequestPath(request)));
	}

	@ExceptionHandler({ HttpClientErrorException.class, ResourceAccessException.class, ApiServiceException.class })
	public ResponseEntity<ApiErrorResponse> handleExternalApiErrors(Exception ex, HttpServletRequest request) {
		HttpStatus status = (ex instanceof HttpClientErrorException httpEx) ? (HttpStatus) httpEx.getStatusCode()
				: HttpStatus.BAD_GATEWAY;

		log.warn("External API error: {}", ex.getMessage(), ex);
		return ResponseEntity.status(status)
			.body(ApiErrorResponse.of(status, ex.getMessage(), getRequestPath(request)));
	}

	@ExceptionHandler({ HighlightsNotFoundException.class, PairsHighlightsNotFoundException.class })
	public ResponseEntity<ApiErrorResponse> handleNotFound(RuntimeException ex, HttpServletRequest request) {
		log.warn("Not found: {}", ex.getMessage(), ex);
		return ResponseEntity.status(HttpStatus.NOT_FOUND)
			.body(ApiErrorResponse.of(HttpStatus.NOT_FOUND, ex.getMessage(), getRequestPath(request)));
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiErrorResponse> handleGenericException(Exception ex, HttpServletRequest request) {
		log.error("Unexpected error", ex);
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
			.body(ApiErrorResponse.of(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error", getRequestPath(request)));
	}

}
