package com.abe.gg_stats.service;

import com.abe.gg_stats.util.LoggingConstants;
import com.abe.gg_stats.util.LoggingUtils;
import com.abe.gg_stats.util.MDCLoggingContext;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Service;

/**
 * Service for collecting and managing application performance metrics. Provides thread-safe metrics
 * collection with automatic aggregation.
 */
@Service
public class MetricsService {

  // Performance metrics storage
  private final Map<String, AtomicLong> operationCounts = new ConcurrentHashMap<>();
  private final Map<String, AtomicLong> totalOperationTime = new ConcurrentHashMap<>();
  private final Map<String, AtomicLong> maxOperationTime = new ConcurrentHashMap<>();
  private final Map<String, AtomicLong> minOperationTime = new ConcurrentHashMap<>();

  /**
   * Records an operation execution with timing information.
   *
   * @param operation  the name of the operation
   * @param durationMs the duration in milliseconds
   */
  public void recordOperation(String operation, long durationMs) {
    if (operation == null || operation.trim().isEmpty()) {
      LoggingUtils.logWarning("Attempted to record operation with null or empty name");
      return;
    }

    if (durationMs < 0) {
      LoggingUtils.logWarning("Attempted to record operation with negative duration: " + durationMs + "ms for operation: " + operation);
      return;
    }

    // Update operation count
    operationCounts.computeIfAbsent(operation, k -> new AtomicLong(0)).incrementAndGet();

    // Update total time
    totalOperationTime.computeIfAbsent(operation, k -> new AtomicLong(0)).addAndGet(durationMs);

    // Update max time
    maxOperationTime.compute(operation, (k, v) -> {
      if (v == null || durationMs > v.get()) {
        return new AtomicLong(durationMs);
      }
      return v;
    });

    // Update min time
    minOperationTime.compute(operation, (k, v) -> {
      if (v == null || durationMs < v.get()) {
        return new AtomicLong(durationMs);
      }
      return v;
    });

    // Log slow operations
    if (durationMs > LoggingConstants.DEFAULT_PERFORMANCE_THRESHOLD_MS) {
      LoggingUtils.logWarning("🐌 Slow operation detected: " + operation + " completed in " + durationMs + "ms");
    } else if (durationMs > LoggingConstants.DEFAULT_WARNING_THRESHOLD_MS) {
      LoggingUtils.logWarning("⚠️ Operation slower than expected: " + operation + " completed in " + durationMs + "ms");
    }
  }

  /**
   * Records an API call with response information.
   *
   * @param endpoint     the API endpoint
   * @param statusCode   the HTTP status code
   * @param responseTime the response time in milliseconds
   * @param responseSize the response size in bytes (optional)
   */
  public void recordApiCall(String endpoint, int statusCode, long responseTime, long responseSize) {
    String operation = "api_" + sanitizeEndpoint(endpoint);

    // Record the operation timing
    recordOperation(operation, responseTime);

    // Log API response details
    String icon = statusCode < 300 ? "✅" : statusCode < 500 ? "⚠️" : "❌";
    LoggingUtils.logDebug(icon + " API response from " + endpoint + ": status=" + statusCode + ", time=" + responseTime + "ms, size=" + responseSize + "bytes");
  }

  /**
   * Records an API call without response size.
   *
   * @param endpoint     the API endpoint
   * @param statusCode   the HTTP status code
   * @param responseTime the response time in milliseconds
   */
  public void recordApiCall(String endpoint, int statusCode, long responseTime) {
    recordApiCall(endpoint, statusCode, responseTime, -1);
  }

  /**
   * Gets comprehensive performance metrics for all operations.
   *
   * @return a map containing performance metrics
   */
  public Map<String, Object> getPerformanceMetrics() {
    Map<String, Object> metrics = new ConcurrentHashMap<>();

    operationCounts.forEach((operation, count) -> {
      long totalTime = totalOperationTime.getOrDefault(operation, new AtomicLong(0)).get();
      long maxTime = maxOperationTime.getOrDefault(operation, new AtomicLong(0)).get();
      long minTime = minOperationTime.getOrDefault(operation, new AtomicLong(0)).get();
      long countValue = count.get();

      double avgTime = countValue > 0 ? (double) totalTime / countValue : 0.0;

      metrics.put(operation + "_count", countValue);
      metrics.put(operation + "_avg_duration_ms", Math.round(avgTime * 100.0) / 100.0);
      metrics.put(operation + "_total_duration_ms", totalTime);
      metrics.put(operation + "_max_duration_ms", maxTime);
      metrics.put(operation + "_min_duration_ms", minTime);
    });

    return metrics;
  }

  /**
   * Gets metrics for a specific operation.
   *
   * @param operation the operation name
   * @return a map containing metrics for the operation, or null if not found
   */
  public Map<String, Object> getOperationMetrics(String operation) {
    if (!operationCounts.containsKey(operation)) {
      return null;
    }

    Map<String, Object> metrics = new ConcurrentHashMap<>();
    long count = operationCounts.get(operation).get();
    long totalTime = totalOperationTime.get(operation).get();
    long maxTime = maxOperationTime.get(operation).get();
    long minTime = minOperationTime.get(operation).get();

    double avgTime = count > 0 ? (double) totalTime / count : 0.0;

    metrics.put("count", count);
    metrics.put("avg_duration_ms", Math.round(avgTime * 100.0) / 100.0);
    metrics.put("total_duration_ms", totalTime);
    metrics.put("max_duration_ms", maxTime);
    metrics.put("min_duration_ms", minTime);

    return metrics;
  }

  /**
   * Resets all metrics.
   */
  public void resetMetrics() {
    operationCounts.clear();
    totalOperationTime.clear();
    maxOperationTime.clear();
    minOperationTime.clear();
    LoggingUtils.logOperationSuccess("Reset all performance metrics");
  }

  /**
   * Resets metrics for a specific operation.
   *
   * @param operation the operation name
   */
  public void resetOperationMetrics(String operation) {
    operationCounts.remove(operation);
    totalOperationTime.remove(operation);
    maxOperationTime.remove(operation);
    minOperationTime.remove(operation);
    LoggingUtils.logOperationSuccess("Reset metrics for operation: " + operation);
  }

  /**
   * Gets a summary of the most expensive operations.
   *
   * @param limit the maximum number of operations to return
   * @return a map of operation names to their average duration, sorted by duration
   */
  public Map<String, Double> getTopExpensiveOperations(int limit) {
    // Create a list of operation metrics for sorting
    List<OperationMetric> operationMetrics = operationCounts.entrySet().stream()
        .filter(entry -> entry.getValue().get() > 0)
        .map(entry -> {
          String operation = entry.getKey();
          long count = entry.getValue().get();
          long totalTime = totalOperationTime.get(operation).get();
          double avgTime = (double) totalTime / count;
          return new OperationMetric(operation, avgTime);
        })
        .sorted(Comparator.comparing(OperationMetric::averageDuration).reversed())
        .limit(limit)
        .toList();

    // Convert to map
    Map<String, Double> result = new ConcurrentHashMap<>();
    operationMetrics.forEach(metric ->
        result.put(metric.operationName(), metric.averageDuration()));

    return result;
  }

  // Private helper methods

  private String sanitizeEndpoint(String endpoint) {
    if (endpoint == null) {
      return "unknown";
    }

    // Remove query parameters and sanitize for logging
    String sanitized = endpoint.split("\\?")[0];

    // Replace path parameters with placeholders for better grouping
    sanitized = sanitized.replaceAll("/\\d+", "/{id}");

    return sanitized;
  }

  /**
   * Inner class to hold operation metric data for sorting
   */
  private record OperationMetric(String operationName, double averageDuration) {

  }
}
