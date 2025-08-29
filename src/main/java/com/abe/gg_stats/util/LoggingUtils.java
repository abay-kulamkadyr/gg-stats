package com.abe.gg_stats.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StopWatch;

/**
 * Logging utility providing consistent, structured logging patterns with performance optimization
 * and security considerations. Features: - Lazy evaluation of log parameters - Structured logging
 * with MDC context - Performance metrics collection - Security-aware parameter sanitization -
 * Thread-safe operation counters
 */
@Slf4j
public final class LoggingUtils {

  private static final String SENSITIVE_DATA_MASK = "[REDACTED]";

  private static final int MAX_PARAM_LENGTH = 200;

  private LoggingUtils() {
    throw new UnsupportedOperationException("Utility class cannot be instantiated");
  }

  @SafeVarargs
  public static void logMethodEntry(String methodName, Supplier<Object>... paramSuppliers) {
    if (log.isDebugEnabled()) {
      Object[] params = evaluateSuppliers(paramSuppliers);
      log.debug("→ Entering {} with params: {}", methodName, formatParams(params));
    }
  }

  public static void logMethodExit(String methodName, Supplier<Object> resultSupplier) {
    if (log.isDebugEnabled()) {
      Object result = resultSupplier != null ? resultSupplier.get() : "void";
      log.debug("← Exiting {} with result: {}", methodName, sanitizeParameter(result));
    }
  }

  public static void logOperationSuccess(String operation) {
    log.info("✅ Successfully completed {}", operation);
  }

  public static void logOperationSuccess(String operation, Object... details) {
    log.info("✅ Successfully completed {} - Details: {}", operation, formatDetails(details));
  }
  
  public static void logOperationStart(String operation, Object... context) {
    log.info("🚀 Starting operation: {} - Context: {}", operation, formatDetails(context));
  }

  public static void logOperationFailure(String operation) {
    log.error("❌ Failed to complete {}", operation);
  }

  public static void logOperationFailure(String operation, Object... details) {
    log.error("❌ Failed to complete {} - Details: {}", operation, formatDetails(details));
  }

  public static void logOperationFailure(String operation, String reason, Throwable error,
      Object... details) {
    log.error("❌ Failed to complete {} - Reason: {} - Details: {} - Error: {}", operation, reason,
        formatDetails(details), error.toString());
  }

  public static void logWarning(String message) {
    log.warn("⚠️ {}", message);
  }

  public static void logWarning(String message, Object... details) {
    log.warn("⚠️ {} - Details: {}", message, formatDetails(details));
  }

  @SafeVarargs
  public static void logDebug(String message, Supplier<Object>... paramSuppliers) {
    if (log.isDebugEnabled()) {
      Object[] params = evaluateSuppliers(paramSuppliers);
      log.debug(message, params);
    }
  }

  public static void logDebug(String message, Object... params) {
    if (log.isDebugEnabled()) {
      log.debug(message, params);
    }
  }

  public static AutoCloseableStopWatch createStopWatch(String operationName) {
    return new AutoCloseableStopWatch(operationName);
  }

  public static void logOperationTiming(StopWatch stopWatch) {
    if (stopWatch.isRunning()) {
      stopWatch.stop();
    }

    String operation = stopWatch.getId();
    long duration = stopWatch.getTotalTimeMillis();

    // Log operation timing with proper MDC context preservation
    if (duration > LoggingConstants.DEFAULT_PERFORMANCE_THRESHOLD_MS) {
      // Log slow operations as warnings
      LoggingUtils.logWarning("🐌 Slow operation detected: " + operation + " completed in " + duration + "ms");
    } else {
      // Log normal operations as info
      LoggingUtils.logOperationSuccess(operation + " completed in " + duration + "ms");
    }
  }

  public static void logBatchProgress(String operation, int current, int total,
      String additionalInfo) {
    if (shouldLogProgress(current, total)) {
      double percentage = ((double) current / total) * 100;
      double rate = calculateProcessingRate(current);

      log.info("📊 {} progress: {}/{} ({}%) - Rate: {} items/sec - {}", operation, current, total,
          percentage,
          rate, additionalInfo != null ? additionalInfo : "");
    }
  }

  public static void logBatchProgress(String operation, int current, int total) {
    logBatchProgress(operation, current, total, null);
  }

  public static void logApiCall(String endpoint, String method, Map<String, Object> headers,
      Object... params) {
    if (log.isDebugEnabled()) {
      log.debug("📡 API {} call to {} - Headers: {} - Params: {}", method, endpoint,
          sanitizeHeaders(headers),
          formatParams(params));
    }
  }

  public static void logApiCall(String endpoint, String method, Object... params) {
    logApiCall(endpoint, method, null, params);
  }

  public static void logApiResponse(String endpoint, int statusCode, long responseTime,
      long responseSize) {
    String icon = statusCode < 300 ? "✅" : statusCode < 500 ? "⚠️" : "❌";

    if (log.isDebugEnabled()) {
      log.debug("{} API response from {}: status={}, time={}ms, size={}bytes", icon, endpoint,
          statusCode,
          responseTime, responseSize);
    }

    // API performance tracking moved to MetricsService
  }

  public static void logApiResponse(String endpoint, int statusCode, long responseTime) {
    logApiResponse(endpoint, statusCode, responseTime, -1);
  }

  @SafeVarargs
  private static Object[] evaluateSuppliers(Supplier<Object>... suppliers) {
    if (suppliers == null || suppliers.length == 0) {
      return new Object[0];
    }

    Object[] results = new Object[suppliers.length];
    for (int i = 0; i < suppliers.length; i++) {
      try {
        results[i] = suppliers[i] != null ? suppliers[i].get() : null;
      } catch (Exception e) {
        results[i] = "[ERROR: " + e.getMessage() + "]";
      }
    }
    return results;
  }

  private static String formatParams(Object... params) {
    if (params == null || params.length == 0) {
      return "none";
    }

    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < params.length; i++) {
      if (i > 0) {
        sb.append(", ");
      }
      sb.append("param").append(i + 1).append("=").append(sanitizeParameter(params[i]));
    }
    return sb.toString();
  }

  private static String formatDetails(Object... details) {
    if (details == null || details.length == 0) {
      return "";
    }

    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < details.length; i++) {
      if (i > 0) {
        sb.append(", ");
      }
      sb.append(sanitizeParameter(details[i]));
    }
    return sb.toString();
  }

  static Object sanitizeParameter(Object param) {
    if (param == null) {
      return "null";
    }

    String paramStr = param.toString();

    // Check for potentially sensitive data patterns
    if (containsSensitiveData(paramStr)) {
      return SENSITIVE_DATA_MASK;
    }

    // Truncate long parameters
    if (paramStr.length() > MAX_PARAM_LENGTH) {
      return paramStr.substring(0, MAX_PARAM_LENGTH) + "...[truncated]";
    }

    return paramStr;
  }

  private static boolean containsSensitiveData(String value) {
    String lowerValue = value.toLowerCase();
    return lowerValue.contains("password") || lowerValue.contains("token") || lowerValue.contains(
        "secret")
        || lowerValue.contains("key=") || lowerValue.matches(".*\\b\\d{13,19}\\b.*"); // Credit
    // card
    // patterns
  }

  private static Map<String, Object> sanitizeHeaders(Map<String, Object> headers) {
    if (headers == null) {
      return null;
    }

    Map<String, Object> sanitized = new ConcurrentHashMap<>();
    headers.forEach((key, value) -> {
      String lowerKey = key.toLowerCase();
      if (lowerKey.contains("auth") || lowerKey.contains("token") || lowerKey.contains("key")) {
        sanitized.put(key, SENSITIVE_DATA_MASK);
      } else {
        sanitized.put(key, value);
      }
    });
    return sanitized;
  }

  private static boolean shouldLogProgress(int current, int total) {
    if (current == total) {
      return true; // Always log completion
    }
    if (total <= 100) {
      return current % 10 == 0; // Every 10% for small batches
    }
    if (total <= 1000) {
      return current % 100 == 0; // Every 100 for medium batches
    }
    return current % 1000 == 0; // Every 1000 for large batches
  }

  private static double calculateProcessingRate(int processed) {
    // Simple rate calculation - in real implementation,
    // you'd track timestamps for more accurate rates
    return processed / Math.max(1.0, System.currentTimeMillis() / 1000.0);
  }

  @Getter
  public static class AutoCloseableStopWatch implements AutoCloseable {

    private final StopWatch stopWatch;

    public AutoCloseableStopWatch(String operationName) {
      this.stopWatch = new StopWatch(operationName);
      this.stopWatch.start();
    }

    @Override
    public void close() {
      LoggingUtils.logOperationTiming(stopWatch);
    }

  }

}
