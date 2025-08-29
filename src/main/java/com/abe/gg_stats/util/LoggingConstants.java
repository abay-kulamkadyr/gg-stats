package com.abe.gg_stats.util;

/**
 * Centralized constants for logging operations and MDC keys.
 * Provides type safety and prevents typos in operation names.
 */
public final class LoggingConstants {
    
    // MDC Keys
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
    
    // Operation Types
    public static final String OPERATION_TYPE_BATCH = "BATCH";
    public static final String OPERATION_TYPE_API_CALL = "API_CALL";
    public static final String OPERATION_TYPE_SERVICE = "SERVICE";
    public static final String OPERATION_TYPE_CIRCUIT_BREAKER = "CIRCUIT_BREAKER";
    public static final String OPERATION_TYPE_RATE_LIMIT = "RATE_LIMIT";
    public static final String OPERATION_TYPE_APPLICATION_STARTUP = "APPLICATION_STARTUP";
    
    // Common Operations
    public static final String OPERATION_USER_CREATION = "UserCreation";
    public static final String OPERATION_USER_UPDATE = "UserUpdate";
    public static final String OPERATION_USER_DELETION = "UserDeletion";
    public static final String OPERATION_BATCH_PROCESSING = "BatchProcessing";
    public static final String OPERATION_API_REQUEST = "ApiRequest";
    public static final String OPERATION_DATA_SYNC = "DataSync";
    public static final String OPERATION_CACHE_UPDATE = "CacheUpdate";
    public static final String OPERATION_CONFIGURATION_VALIDATION = "ConfigurationValidation";
    
    // Batch Operations
    public static final String OPERATION_BATCH_READ = "BatchRead";
    public static final String OPERATION_BATCH_PROCESS = "BatchProcess";
    public static final String OPERATION_BATCH_WRITE = "BatchWrite";
    public static final String OPERATION_BATCH_INITIALIZE = "BatchInitialize";
    public static final String OPERATION_BATCH_VALIDATION = "BatchValidation";
    
    // Performance Thresholds
    public static final long DEFAULT_PERFORMANCE_THRESHOLD_MS = 5000L;
    public static final long DEFAULT_WARNING_THRESHOLD_MS = 2500L;
    
    private LoggingConstants() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }
}

