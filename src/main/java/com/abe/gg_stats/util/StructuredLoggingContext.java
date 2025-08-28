package com.abe.gg_stats.util;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.lang.NonNull;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Centralized management for structured logging context using MDC. Provides thread-safe
 * operations for setting and managing logging context across different types of
 * operations (batch, API calls, service operations).
 *
 * Features: - Automatic correlation ID generation - Context inheritance for nested
 * operations - Thread-safe context management - Automatic cleanup to prevent memory leaks
 * - Operation-specific context builders
 */
@Slf4j
public final class StructuredLoggingContext {

	// Standard MDC keys
	public static final String CORRELATION_ID = "correlationId";

	public static final String OPERATION_TYPE = "operationType";

	public static final String OPERATION_NAME = "operationName";

	public static final String JOB_ID = "jobId";

	public static final String STEP_NAME = "stepName";

	public static final String BATCH_TYPE = "batchType";

	public static final String API_ENDPOINT = "apiEndpoint";

	public static final String HTTP_METHOD = "httpMethod";

	public static final String SERVICE_NAME = "serviceName";

	public static final String THREAD_NAME = "threadName";

	public static final String START_TIME = "startTime";

	private StructuredLoggingContext() {
		throw new UnsupportedOperationException("Utility class cannot be instantiated");
	}

	/**
	 * Sets up context for batch operations with automatic correlation ID generation
	 */
	public static String setBatchContext(@NonNull String batchType, String jobId, String stepName) {
		String correlationId = getOrCreateCorrelationId();

		Map<String, String> context = new ConcurrentHashMap<>();
		context.put(CORRELATION_ID, correlationId);
		context.put(OPERATION_TYPE, "BATCH");
		context.put(BATCH_TYPE, batchType);
		context.put(THREAD_NAME, Thread.currentThread().getName());
		context.put(START_TIME, Instant.now().toString());

		if (jobId != null)
			context.put(JOB_ID, jobId);
		if (stepName != null)
			context.put(STEP_NAME, stepName);

		setMDCContext(context);
		return correlationId;
	}

	/**
	 * Sets up context for API operations
	 */
	public static String setApiContext(@NonNull String endpoint, String method) {
		String correlationId = getOrCreateCorrelationId();

		Map<String, String> context = new ConcurrentHashMap<>();
		context.put(CORRELATION_ID, correlationId);
		context.put(OPERATION_TYPE, "API_CALL");
		context.put(API_ENDPOINT, sanitizeEndpoint(endpoint));
		context.put(THREAD_NAME, Thread.currentThread().getName());
		context.put(START_TIME, Instant.now().toString());

		if (method != null)
			context.put(HTTP_METHOD, method.toUpperCase());

		setMDCContext(context);
		return correlationId;
	}

	/**
	 * Sets up context for service operations
	 */
	public static String setServiceContext(@NonNull String serviceName, @NonNull String operationName) {
		String correlationId = getOrCreateCorrelationId();

		Map<String, String> context = new ConcurrentHashMap<>();
		context.put(CORRELATION_ID, correlationId);
		context.put(OPERATION_TYPE, "SERVICE");
		context.put(SERVICE_NAME, serviceName);
		context.put(OPERATION_NAME, operationName);
		context.put(THREAD_NAME, Thread.currentThread().getName());
		context.put(START_TIME, Instant.now().toString());

		setMDCContext(context);
		return correlationId;
	}

	/**
	 * Sets up context for circuit breaker operations
	 */
	public static String setCircuitBreakerContext(@NonNull String serviceName, @NonNull String state) {
		String correlationId = getOrCreateCorrelationId();

		Map<String, String> context = new ConcurrentHashMap<>();
		context.put(CORRELATION_ID, correlationId);
		context.put(OPERATION_TYPE, "CIRCUIT_BREAKER");
		context.put(SERVICE_NAME, serviceName);
		context.put("circuitBreakerState", state);
		context.put(THREAD_NAME, Thread.currentThread().getName());

		setMDCContext(context);
		return correlationId;
	}

	/**
	 * Sets up context for rate limiting operations
	 */
	public static String setRateLimitContext(@NonNull String endpoint, int remainingTokens) {
		String correlationId = getOrCreateCorrelationId();

		Map<String, String> context = new ConcurrentHashMap<>();
		context.put(CORRELATION_ID, correlationId);
		context.put(OPERATION_TYPE, "RATE_LIMIT");
		context.put(API_ENDPOINT, sanitizeEndpoint(endpoint));
		context.put("remainingTokens", String.valueOf(remainingTokens));
		context.put(THREAD_NAME, Thread.currentThread().getName());

		setMDCContext(context);
		return correlationId;
	}

	/**
	 * Updates the current context with additional data
	 */
	public static void updateContext(@NonNull Map<String, String> additionalData) {
		additionalData.forEach((key, value) -> {
			if (value != null) {
				MDC.put(key, LoggingUtils.sanitizeParameter(value).toString());
			}
		});
	}

	/**
	 * Updates a single context value
	 */
	public static void updateContext(@NonNull String key, String value) {
		if (value != null) {
			MDC.put(key, LoggingUtils.sanitizeParameter(value).toString());
		}
	}

	/**
	 * Gets the current correlation ID or creates a new one
	 */
	public static String getOrCreateCorrelationId() {
		String existing = MDC.get(CORRELATION_ID);
		if (existing != null && !existing.isEmpty()) {
			return existing;
		}
		return generateCorrelationId();
	}

	/**
	 * Gets the current correlation ID without creating a new one
	 */
	public static String getCurrentCorrelationId() {
		return MDC.get(CORRELATION_ID);
	}

	/**
	 * Gets all current MDC context as a map
	 */
	public static Map<String, String> getCurrentContext() {
		return MDC.getCopyOfContextMap();
	}

	/**
	 * Inherits context from a parent thread/operation
	 */
	public static void inheritContext(@NonNull Map<String, String> parentContext) {
		// Clear current context first
		clearContext();

		// Set inherited context
		parentContext.forEach((key, value) -> {
			if (value != null) {
				MDC.put(key, value);
			}
		});

		// Update thread name to current thread
		MDC.put(THREAD_NAME, Thread.currentThread().getName());
	}

	/**
	 * Creates a scoped context that automatically cleans up
	 */
	public static ScopedContext createScopedContext(@NonNull String operationType, @NonNull String identifier) {
		return new ScopedContext(operationType, identifier);
	}

	/**
	 * Safely clears all MDC context
	 */
	public static void clearContext() {
		try {
			MDC.clear();
		}
		catch (Exception e) {
			LoggingUtils.logWarning("StructuredLoggingContext", "Error clearing MDC context", e.getMessage());
		}
	}

	// Private helper methods

	private static String generateCorrelationId() {
		return UUID.randomUUID().toString().replace("-", "").substring(0, 8);
	}

	private static void setMDCContext(Map<String, String> context) {
		context.forEach((key, value) -> {
			if (value != null) {
				MDC.put(key, LoggingUtils.sanitizeParameter(value).toString());
			}
		});
	}

	private static String sanitizeEndpoint(String endpoint) {
		if (endpoint == null)
			return null;

		// Remove query parameters and sanitize for logging
		String sanitized = endpoint.split("\\?")[0];

		// Replace path parameters with placeholders for better grouping
		sanitized = sanitized.replaceAll("/\\d+", "/{id}");

		return sanitized;
	}

	/**
	 * Auto-closeable scoped context for use with try-with-resources
	 */
	public static class ScopedContext implements AutoCloseable {

		private final Map<String, String> previousContext;

		private ScopedContext(String operationType, String identifier) {
			// Save previous context
			this.previousContext = MDC.getCopyOfContextMap();

			// Set new scoped context
			String correlationId = getOrCreateCorrelationId();
			MDC.put(CORRELATION_ID, correlationId);
			MDC.put(OPERATION_TYPE, operationType);
			MDC.put(OPERATION_NAME, identifier);
			MDC.put(THREAD_NAME, Thread.currentThread().getName());
			MDC.put(START_TIME, Instant.now().toString());
		}

		@Override
		public void close() {
			// Restore previous context
			MDC.clear();
			if (previousContext != null) {
				previousContext.forEach(MDC::put);
			}
		}

		public String getCorrelationId() {
			return MDC.get(CORRELATION_ID);
		}

	}

}
