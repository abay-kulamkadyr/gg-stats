package com.abe.gg_stats.util;

/**
 * Mixin interface providing consistent logging patterns for service classes. Implements
 * standardized service operation logging with structured context management.
 * <p>
 * Usage: Have your service classes implement this interface to get consistent logging
 * patterns with automatic correlation tracking and structured context.
 */
public interface ServiceLogging {

	/**
	 * Gets the service name for logging context
	 */
	default String getServiceName() {
		return this.getClass().getSimpleName();
	}

	/**
	 * Gets the performance threshold for slow operation detection (in milliseconds)
	 * Override in implementing classes for service-specific thresholds
	 */
	default long getPerformanceThreshold() {
		return 5000; // Default: 5 seconds
	}

	/**
	 * Gets the warning threshold for moderately slow operations (in milliseconds)
	 */
	default long getWarningThreshold() {
		return getPerformanceThreshold() / 2; // Default: half of performance threshold
	}

	/**
	 * Logs the start of a service operation with structured context
	 */
	default String logServiceStart(String operation, Object... context) {
		String correlationId = StructuredLoggingContext.setServiceContext(getServiceName(), operation);
		LoggingUtils.logOperationStart(operation, context);
		return correlationId;
	}

	/**
	 * Logs successful completion of a service operation
	 */
	default void logServiceSuccess(String operation, Object... context) {
		LoggingUtils.logOperationSuccess(operation, context);
		StructuredLoggingContext.clearContext();
	}

	/**
	 * Logs successful completion of a service operation with duration
	 */
	default void logServiceSuccess(String operation, long durationMs, Object... context) {
		// Check for performance issues
		if (durationMs > getPerformanceThreshold()) {
			LoggingUtils.logWarning("Slow service operation detected", "service=" + getServiceName(),
					"operation=" + operation, "duration=" + durationMs + "ms",
					"threshold=" + getPerformanceThreshold() + "ms");
		}
		else if (durationMs > getWarningThreshold()) {
			LoggingUtils.logWarning("Service operation slower than expected", "service=" + getServiceName(),
					"operation=" + operation, "duration=" + durationMs + "ms",
					"warningThreshold=" + getWarningThreshold() + "ms");
		}

		LoggingUtils.logOperationSuccess(operation, durationMs, context);
		StructuredLoggingContext.clearContext();
	}

	/**
	 * Logs service operation failure
	 */
	default void logServiceFailure(String operation, String reason, Object... context) {
		LoggingUtils.logOperationFailure(operation, reason);
		StructuredLoggingContext.clearContext();
	}

	/**
	 * Logs service operation failure with exception
	 */
	default void logServiceFailure(String operation, String reason, Throwable error, Object... context) {
		LoggingUtils.logOperationFailure(operation, reason, error);
		StructuredLoggingContext.clearContext();
	}

	/**
	 * Logs service warning
	 */
	default void logServiceWarning(String message, Object... context) {
		LoggingUtils.logWarning(message, context);
	}

	/**
	 * Creates a scoped context for complex operations that automatically cleans up
	 */
	default StructuredLoggingContext.ScopedContext createScopedOperation(String operation) {
		return StructuredLoggingContext.createScopedContext("SERVICE", getServiceName() + "." + operation);
	}

	/**
	 * Executes an operation with automatic timing and logging
	 */
	default <T> T executeWithLogging(String operation, ServiceOperation<T> serviceOperation, Object... context) {
		String correlationId = logServiceStart(operation, context);
		long startTime = System.currentTimeMillis();

		try (LoggingUtils.AutoCloseableStopWatch stopWatch = LoggingUtils.createStopWatch(operation)) {
			T result = serviceOperation.execute();

			long duration = System.currentTimeMillis() - startTime;
			logServiceSuccess(operation, duration, new Object[] { "correlationId=" + correlationId });
			return result;
		}
		catch (RuntimeException e) {
			long duration = System.currentTimeMillis() - startTime;
			logServiceFailure(operation, "Operation failed", e, "correlationId=" + correlationId,
					"duration=" + duration + "ms");
			throw e;
		}
		catch (Exception e) {
			long duration = System.currentTimeMillis() - startTime;
			logServiceFailure(operation, "Operation failed", e, "correlationId=" + correlationId,
					"duration=" + duration + "ms");
			throw new RuntimeException("Service operation failed: " + operation, e);
		}
	}

	/**
	 * Executes an operation with automatic timing and logging (void operations)
	 */
	default void executeWithLogging(String operation, VoidServiceOperation serviceOperation, Object... context) {
		String correlationId = logServiceStart(operation, context);
		long startTime = System.currentTimeMillis();

		try (LoggingUtils.AutoCloseableStopWatch stopWatch = LoggingUtils.createStopWatch(operation)) {
			serviceOperation.execute();

			long duration = System.currentTimeMillis() - startTime;
			logServiceSuccess(operation, duration, new Object[] { "correlationId=" + correlationId });
		}
		catch (RuntimeException e) {
			long duration = System.currentTimeMillis() - startTime;
			logServiceFailure(operation, "Operation failed", e, "correlationId=" + correlationId,
					"duration=" + duration + "ms");
			throw e;
		}
		catch (Exception e) {
			long duration = System.currentTimeMillis() - startTime;
			logServiceFailure(operation, "Operation failed", e, "correlationId=" + correlationId,
					"duration=" + duration + "ms");
			throw new RuntimeException("Service operation failed: " + operation, e);
		}
	}

	/**
	 * Functional interface for service operations that return a value
	 */
	@FunctionalInterface
	interface ServiceOperation<T> {

		T execute() throws Exception;

	}

	/**
	 * Functional interface for void service operations
	 */
	@FunctionalInterface
	interface VoidServiceOperation {

		void execute() throws Exception;

	}

}
