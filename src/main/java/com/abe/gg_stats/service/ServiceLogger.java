package com.abe.gg_stats.service;

import com.abe.gg_stats.util.LoggingConstants;
import com.abe.gg_stats.util.LoggingUtils;
import com.abe.gg_stats.util.MDCLoggingContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Spring-managed component for service operation logging. Provides consistent logging
 * patterns with automatic correlation tracking, performance monitoring, and structured
 * context management.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ServiceLogger {

	private final MetricsService metricsService;

	/**
	 * Executes a service operation with automatic logging, timing, and context
	 * management.
	 * @param serviceName the name of the service
	 * @param operation the operation being performed
	 * @param serviceOperation the operation to execute
	 * @param context additional context information
	 * @param <T> the return type of the operation
	 * @return the result of the operation
	 * @throws RuntimeException if the operation fails
	 */
	public <T> T executeWithLogging(String serviceName, String operation, ServiceOperation<T> serviceOperation,
			Object... context) {
		String correlationId = MDCLoggingContext.setServiceContext(serviceName, operation);

		try (LoggingUtils.AutoCloseableStopWatch stopWatch = LoggingUtils.createStopWatch(operation)) {
			T result = serviceOperation.execute();

			// Capture duration before any potential exception
			long duration = stopWatch.getStopWatch().getTotalTimeMillis();

			// Record metrics for successful operation
			metricsService.recordOperation(operation, duration);

			logServiceSuccess(serviceName, operation, duration, correlationId, context);

			return result;
		}
		catch (Exception e) {
			logServiceFailure(operation, "Unexpected exception occurred", e);
			throw new RuntimeException();
		}
		finally {
			MDCLoggingContext.clearContext();
		}
	}

	/**
	 * Executes a void service operation with automatic logging, timing, and context
	 * management.
	 * @param serviceName the name of the service
	 * @param operation the operation being performed
	 * @param serviceOperation the operation to execute
	 * @param context additional context information
	 * @throws RuntimeException if the operation fails
	 */
	public void executeWithLogging(String serviceName, String operation, VoidServiceOperation serviceOperation,
			Object... context) {
		String correlationId = MDCLoggingContext.setServiceContext(serviceName, operation);

		try (LoggingUtils.AutoCloseableStopWatch stopWatch = LoggingUtils.createStopWatch(operation)) {
			serviceOperation.execute();

			long duration = stopWatch.getStopWatch().getTotalTimeMillis();

			metricsService.recordOperation(operation, duration);

			logServiceSuccess(serviceName, operation, duration, correlationId, context);
		}
		catch (Exception e) {
			logServiceFailure(operation, "Unexpected exception occurred", e);
			throw new RuntimeException();
		}
		finally {
			MDCLoggingContext.clearContext();
		}
	}

	/**
	 * Logs the start of a service operation.
	 * @param operation the operation being performed
	 * @param context additional context information
	 */
	public void logServiceStart(String operation, Object... context) {
		LoggingUtils.logOperationStart(operation, context);
	}

	/**
	 * Logs successful completion of a service operation.
	 * @param operation the operation being performed
	 * @param context additional context information
	 */
	public void logServiceSuccess(String operation, Object... context) {
		LoggingUtils.logOperationSuccess(operation, context);
		MDCLoggingContext.clearContext();
	}

	/**
	 * Logs successful completion of a service operation with duration.
	 * @param serviceName the name of the service
	 * @param operation the operation being performed
	 * @param durationMs the duration in milliseconds
	 * @param correlationId the correlation ID for this operation
	 * @param context additional context information
	 */
	public void logServiceSuccess(String serviceName, String operation, long durationMs, String correlationId,
			Object... context) {
		checkPerformanceThresholds(serviceName, operation, durationMs);

		Object[] enhancedContext = enhanceContext(context, "duration=" + durationMs + "ms",
				"correlationId=" + correlationId);

		LoggingUtils.logOperationSuccess(operation, enhancedContext);
	}

	/**
	 * Logs service operation failure.
	 * @param operation the operation being performed
	 * @param reason the reason for failure
	 */
	public void logServiceFailure(String operation, String reason) {
		LoggingUtils.logOperationFailure(operation, reason);
		MDCLoggingContext.clearContext();
	}

	public void logServiceFailure(String operation, String reason, Throwable error) {
		LoggingUtils.logOperationFailure(operation, reason, error);
	}

	/**
	 * Logs a service warning.
	 * @param message the warning message
	 * @param context additional context information
	 */
	public void logServiceWarning(String message, Object... context) {
		LoggingUtils.logWarning(message, context);
	}

	/**
	 * Creates a scoped context for complex operations.
	 * @param operation the operation name
	 * @return a scoped context that automatically cleans up
	 */
	public MDCLoggingContext.ScopedContext createScopedOperation(String operation) {
		return MDCLoggingContext.createScopedContext(LoggingConstants.OPERATION_TYPE_SERVICE, operation);
	}

	// Private helper methods

	private void checkPerformanceThresholds(String serviceName, String operation, long durationMs) {
		if (durationMs > LoggingConstants.DEFAULT_PERFORMANCE_THRESHOLD_MS) {
			LoggingUtils.logWarning("Slow service operation detected", "service=" + serviceName,
					"operation=" + operation, "duration=" + durationMs + "ms",
					"threshold=" + LoggingConstants.DEFAULT_PERFORMANCE_THRESHOLD_MS + "ms");
		}
		else if (durationMs > LoggingConstants.DEFAULT_WARNING_THRESHOLD_MS) {
			LoggingUtils.logWarning("Service operation slower than expected", "service=" + serviceName,
					"operation=" + operation, "duration=" + durationMs + "ms",
					"warningThreshold=" + LoggingConstants.DEFAULT_WARNING_THRESHOLD_MS + "ms");
		}
	}

	private Object[] enhanceContext(Object[] existingContext, Object... additionalContext) {
		if (existingContext == null || existingContext.length == 0) {
			return additionalContext;
		}

		Object[] enhanced = new Object[existingContext.length + additionalContext.length];
		System.arraycopy(existingContext, 0, enhanced, 0, existingContext.length);
		System.arraycopy(additionalContext, 0, enhanced, existingContext.length, additionalContext.length);

		return enhanced;
	}

	// Functional interfaces

	/**
	 * Functional interface for service operations that return a value.
	 */
	@FunctionalInterface
	public interface ServiceOperation<T> {

		T execute() throws Exception;

	}

	/**
	 * Functional interface for void service operations.
	 */
	@FunctionalInterface
	public interface VoidServiceOperation {

		void execute() throws Exception;

	}

}
