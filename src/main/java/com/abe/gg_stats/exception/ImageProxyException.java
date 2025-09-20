package com.abe.gg_stats.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class ImageProxyException extends RuntimeException {

	private final HttpStatus status;

	public ImageProxyException(String message, HttpStatus status) {
		super(message);
		this.status = status;
	}

	public ImageProxyException(String message, HttpStatus status, Throwable cause) {
		super(message, cause);
		this.status = status;
	}

}