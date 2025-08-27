package com.abe.gg_stats.util;

import org.springframework.util.StopWatch;

/**
 * Mixin interface providing consistent logging patterns for service classes.
 * Implements standardized service operation logging with structured context management.
 * 
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
        
        try (LoggingUtils.AutoCloseableStopWatch stopWatch = LoggingUtils.createStopWatch(operation)) {
            T result = serviceOperation.execute();
            logServiceSuccess(operation, "correlationId=" + correlationId);
            return result;
        } catch (RuntimeException e) {
            logServiceFailure(operation, "Operation failed", e, "correlationId=" + correlationId);
            throw e;
        } catch (Exception e) {
            logServiceFailure(operation, "Operation failed", e, "correlationId=" + correlationId);
            throw new RuntimeException("Service operation failed: " + operation, e);
        }
    }

    /**
     * Executes an operation with automatic timing and logging (void operations)
     */
    default void executeWithLogging(String operation, VoidServiceOperation serviceOperation, Object... context) {
        String correlationId = logServiceStart(operation, context);
        
        try (LoggingUtils.AutoCloseableStopWatch stopWatch = LoggingUtils.createStopWatch(operation)) {
            serviceOperation.execute();
            logServiceSuccess(operation, "correlationId=" + correlationId);
        } catch (RuntimeException e) {
            logServiceFailure(operation, "Operation failed", e, "correlationId=" + correlationId);
            throw e;
        } catch (Exception e) {
            logServiceFailure(operation, "Operation failed", e, "correlationId=" + correlationId);
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
