package com.abe.gg_stats.config;

import com.abe.gg_stats.util.LoggingConstants;
import com.abe.gg_stats.util.LoggingUtils;
import com.abe.gg_stats.util.MDCLoggingContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Listener that sets up initial MDC context during application startup.
 * This ensures that production logs show MDC fields even before batch jobs run.
 */
@Component
@Slf4j
public class ApplicationStartupListener {

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        // Set up initial MDC context for application startup
        String startupCorrelationId = MDCLoggingContext.getOrCreateCorrelationId();
        MDCLoggingContext.updateContext("operationType", LoggingConstants.OPERATION_TYPE_APPLICATION_STARTUP);
        MDCLoggingContext.updateContext("serviceName", "gg-stats");
        MDCLoggingContext.updateContext("correlationId", startupCorrelationId);
        
        		LoggingUtils.logOperationSuccess("Application startup", "correlationId=" + startupCorrelationId);
        
        // Set operation name for better context
        MDCLoggingContext.updateContext("operationName", LoggingConstants.OPERATION_TYPE_APPLICATION_STARTUP);
        
        // Note: We don't clear this context as it will be overridden by batch jobs
        // when they start executing
    }
}
