package com.abe.gg_stats.dto.response;

import org.springframework.http.ResponseEntity;

// Create this DTO in a new package, e.g., com.abe.gg_stats.dto.response
public record ImageProxyDto(ResponseEntity<byte[]> responseEntity) {
}