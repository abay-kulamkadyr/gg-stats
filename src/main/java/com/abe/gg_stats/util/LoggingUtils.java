package com.abe.gg_stats.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StopWatch;

/**
 * Utility class for consistent logging patterns across the application. Provides
 * standardized logging methods for common scenarios.
 */
@Slf4j
public final class LoggingUtils {

	private LoggingUtils() {
		throw new UnsupportedOperationException("Utility class");
	}

	/**
	 * Log method entry with parameters (DEBUG level)
	 */
	public static void logMethodEntry(String methodName, Object... params) {
		if (log.isDebugEnabled()) {
			log.debug("Entering {} with params: {}", methodName, formatParams(params));
		}
	}

	/**
	 * Log method exit with result (DEBUG level)
	 */
	public static void logMethodExit(String methodName, Object result) {
		if (log.isDebugEnabled()) {
			log.debug("Exiting {} with result: {}", methodName, result);
		}
	}

	/**
	 * Log method exit without result (DEBUG level)
	 */
	public static void logMethodExit(String methodName) {
		if (log.isDebugEnabled()) {
			log.debug("Exiting {}", methodName);
		}
	}

	/**
	 * Log operation start (INFO level)
	 */
	public static void logOperationStart(String operation, Object... details) {
		log.info("Starting {} - {}", operation, formatDetails(details));
	}

	/**
	 * Log operation success (INFO level)
	 */
	public static void logOperationSuccess(String operation, Object... details) {
		log.info("Successfully completed {} - {}", operation, formatDetails(details));
	}

	/**
	 * Log operation failure (ERROR level)
	 */
	public static void logOperationFailure(String operation, String reason, Throwable error) {
		log.error("Failed to complete {} - Reason: {}", operation, reason, error);
	}

	/**
	 * Log operation failure without exception (ERROR level)
	 */
	public static void logOperationFailure(String operation, String reason) {
		log.error("Failed to complete {} - Reason: {}", operation, reason);
	}

	/**
	 * Log warning with context (WARN level)
	 */
	public static void logWarning(String message, Object... context) {
		log.warn("{} - Context: {}", message, formatDetails(context));
	}

	/**
	 * Log debug information only if debug is enabled
	 */
	public static void logDebug(String message, Object... params) {
		if (log.isDebugEnabled()) {
			log.debug(message, params);
		}
	}

	/**
	 * Log trace information only if trace is enabled
	 */
	public static void logTrace(String message, Object... params) {
		if (log.isTraceEnabled()) {
			log.trace(message, params);
		}
	}

	/**
	 * Create a stopwatch for timing operations
	 */
	public static StopWatch createStopWatch(String operationName) {
		StopWatch stopWatch = new StopWatch(operationName);
		stopWatch.start();
		return stopWatch;
	}

	/**
	 * Log operation timing (INFO level)
	 */
	public static void logOperationTiming(StopWatch stopWatch) {
		if (stopWatch.isRunning()) {
			stopWatch.stop();
		}
		log.info("{} completed in {} ms", stopWatch.getId(), stopWatch.getTotalTimeMillis());
	}

	/**
	 * Log batch processing progress (INFO level)
	 */
	public static void logBatchProgress(String operation, int current, int total) {
		if (current % 100 == 0 || current == total) { // Log every 100 items or at
														// completion
			int percentage = (int) ((double) current / total * 100);
			log.info("{} progress: {}/{} ({}%)", operation, current, total, percentage);
		}
	}

	/**
	 * Log API call details (DEBUG level)
	 */
	public static void logApiCall(String endpoint, String method, Object... params) {
		if (log.isDebugEnabled()) {
			log.debug("API {} call to {} with params: {}", method, endpoint, formatParams(params));
		}
	}

	/**
	 * Log API response (DEBUG level)
	 */
	public static void logApiResponse(String endpoint, int statusCode, long responseTime) {
		if (log.isDebugEnabled()) {
			log.debug("API response from {}: status={}, time={}ms", endpoint, statusCode, responseTime);
		}
	}

	/**
	 * Format parameters for logging
	 */
	private static String formatParams(Object... params) {
		if (params == null || params.length == 0) {
			return "none";
		}

		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < params.length; i++) {
			if (i > 0)
				sb.append(", ");
			sb.append("param").append(i + 1).append("=").append(params[i]);
		}
		return sb.toString();
	}

	/**
	 * Format details for logging
	 */
	private static String formatDetails(Object... details) {
		if (details == null || details.length == 0) {
			return "no details";
		}

		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < details.length; i++) {
			if (i > 0)
				sb.append(", ");
			sb.append(details[i]);
		}
		return sb.toString();
	}

}
