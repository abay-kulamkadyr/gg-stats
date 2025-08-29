package com.abe.gg_stats.service;

import com.abe.gg_stats.exception.ConfigurationException;
import jakarta.annotation.PostConstruct;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Enterprise configuration validation and management service.
 * <p>
 * Features: - Comprehensive configuration validation - Environment-specific validation rules -
 * Configuration recommendations and warnings - Runtime configuration monitoring - Centralized
 * configuration documentation
 */
@Service
@RequiredArgsConstructor
public class ConfigurationService {

  private final ServiceLogger serviceLogger;

  // OpenDota API Configuration
  @Value("${opendota.api.base-url:https://api.opendota.com/api}")
  private String openDotaBaseUrl;

  @Value("${opendota.api.timeout.read:30000}")
  private long readTimeoutMs;

  @Value("${opendota.api.timeout.connect:10000}")
  private long connectTimeoutMs;

  // Rate Limiting Configuration
  @Value("${opendota.api.rate-limit.per-minute:50}")
  private int requestsPerMinute;

  @Value("${opendota.api.rate-limit.per-day:1800}")
  private int requestsPerDay;

  @Value("${opendota.api.rate-limit.burst-capacity:10}")
  private int burstCapacity;

  @Value("${opendota.api.rate-limit.enable-waiting:true}")
  private boolean enableWaiting;

  // Circuit Breaker Configuration
  @Value("${circuit-breaker.failure-threshold:5}")
  private int circuitBreakerFailureThreshold;

  @Value("${circuit-breaker.success-threshold:3}")
  private int circuitBreakerSuccessThreshold;

  @Value("${circuit-breaker.timeout-duration-ms:30000}")
  private long circuitBreakerTimeoutMs;

  @Value("${circuit-breaker.minimum-calls:10}")
  private int minimumCalls;

  @Value("${circuit-breaker.sliding-window-size:100}")
  private int slidingWindowSize;

  // Health Check Configuration
  @Value("${opendota.api.health-check.enabled:true}")
  private boolean healthCheckEnabled;

  @Value("${spring.profiles.active:default}")
  private String activeProfile;

  @PostConstruct
  public void validateConfiguration() {
    serviceLogger.logServiceStart("ConfigurationService", "Configuration validation",
        "profile=" + activeProfile);

    try {
      List<String> validationWarnings = new ArrayList<>();
      List<String> validationErrors = new ArrayList<>();

      // Validate API configuration
      validateApiConfiguration(validationErrors, validationWarnings);

      // Validate rate limiting configuration
      validateRateLimitingConfiguration(validationErrors, validationWarnings);

      // Validate circuit breaker configuration
      validateCircuitBreakerConfiguration(validationErrors, validationWarnings);

      // Validate environment-specific settings
      validateEnvironmentSpecificSettings(validationErrors, validationWarnings);

      // Report results
      reportValidationResults(validationErrors, validationWarnings);

      serviceLogger.logServiceSuccess("ConfigurationService", "Configuration validation completed",
          "errors=" + validationErrors.size(), "warnings=" + validationWarnings.size());

    } catch (Exception e) {
      serviceLogger.logServiceFailure("Configuration validation", "Configuration validation failed",
          e);
      throw new ConfigurationException("validation", "failed",
          "Configuration validation failed: " + e.getMessage());
    }
  }

  /**
   * Get current configuration summary for monitoring
   */
  public ConfigurationSummary getConfigurationSummary() {
    return ConfigurationSummary.builder()
        .activeProfile(activeProfile)
        .apiConfiguration(getApiConfiguration())
        .rateLimitConfiguration(getRateLimitConfiguration())
        .circuitBreakerConfiguration(getCircuitBreakerConfiguration())
        .healthCheckConfiguration(getHealthCheckConfiguration())
        .build();
  }

  /**
   * Validate a specific configuration value at runtime
   */
  public ValidationResult validateConfigurationValue(String key, Object value) {
    try {
      switch (key) {
        case "opendota.api.base-url":
          return validateUrl(key, (String) value);
        case "opendota.api.timeout.read":
        case "opendota.api.timeout.connect":
          return validateTimeout(key, (Long) value);
        case "opendota.api.rate-limit.per-minute":
          return validateRateLimit(key, (Integer) value, 1, 1000);
        case "opendota.api.rate-limit.per-day":
          return validateRateLimit(key, (Integer) value, 1, 50000);
        default:
          return ValidationResult.warning("Unknown configuration key: " + key);
      }
    } catch (Exception e) {
      return ValidationResult.error("Validation failed: " + e.getMessage());
    }
  }

  // Private validation methods

  private void validateApiConfiguration(List<String> errors, List<String> warnings) {
    // Validate base URL
    try {
      new URL(openDotaBaseUrl);
      if (!openDotaBaseUrl.startsWith("https://")) {
        warnings.add("API base URL should use HTTPS for security");
      }
    } catch (MalformedURLException e) {
      errors.add("Invalid API base URL: " + openDotaBaseUrl);
    }

    // Validate timeouts
    if (readTimeoutMs < 1000) {
      errors.add("Read timeout too low: " + readTimeoutMs + "ms (minimum: 1000ms)");
    } else if (readTimeoutMs > 300000) {
      warnings.add("Read timeout very high: " + readTimeoutMs + "ms (recommended: 5000-60000ms)");
    }

    if (connectTimeoutMs < 1000) {
      errors.add("Connect timeout too low: " + connectTimeoutMs + "ms (minimum: 1000ms)");
    } else if (connectTimeoutMs > 30000) {
      warnings.add("Connect timeout high: " + connectTimeoutMs + "ms (recommended: 5000-10000ms)");
    }

    if (readTimeoutMs <= connectTimeoutMs) {
      warnings.add("Read timeout should be higher than connect timeout");
    }
  }

  private void validateRateLimitingConfiguration(List<String> errors, List<String> warnings) {
    if (requestsPerMinute <= 0) {
      errors.add("Requests per minute must be positive: " + requestsPerMinute);
    } else if (requestsPerMinute > 1000) {
      warnings.add(
          "Very high requests per minute: " + requestsPerMinute + " (API may not support this)");
    }

    if (requestsPerDay <= 0) {
      errors.add("Requests per day must be positive: " + requestsPerDay);
    } else if (requestsPerDay < requestsPerMinute * 60) {
      warnings.add("Daily limit inconsistent with minute limit");
    }

    if (burstCapacity <= 0) {
      errors.add("Burst capacity must be positive: " + burstCapacity);
    } else if (burstCapacity > requestsPerMinute) {
      warnings.add("Burst capacity higher than per-minute limit");
    }
  }

  private void validateCircuitBreakerConfiguration(List<String> errors, List<String> warnings) {
    if (circuitBreakerFailureThreshold <= 0) {
      errors.add(
          "Circuit breaker failure threshold must be positive: " + circuitBreakerFailureThreshold);
    }

    if (circuitBreakerSuccessThreshold <= 0) {
      errors.add(
          "Circuit breaker success threshold must be positive: " + circuitBreakerSuccessThreshold);
    }

    if (circuitBreakerTimeoutMs < 1000) {
      errors.add(
          "Circuit breaker timeout too low: " + circuitBreakerTimeoutMs + "ms (minimum: 1000ms)");
    } else if (circuitBreakerTimeoutMs > 300000) {
      warnings.add("Circuit breaker timeout very high: " + circuitBreakerTimeoutMs + "ms");
    }

    if (minimumCalls <= circuitBreakerFailureThreshold) {
      warnings.add("Minimum calls should be higher than failure threshold for accurate statistics");
    }

    if (slidingWindowSize < minimumCalls) {
      errors.add("Sliding window size must be at least equal to minimum calls");
    }
  }

  private void validateEnvironmentSpecificSettings(List<String> errors, List<String> warnings) {
    switch (activeProfile.toLowerCase()) {
      case "prod":
      case "production":
        validateProductionSettings(errors, warnings);
        break;
      case "dev":
      case "development":
        validateDevelopmentSettings(warnings);
        break;
      case "test":
      case "testing":
        validateTestSettings(warnings);
        break;
      default:
        warnings.add("Unknown profile: " + activeProfile);
    }
  }

  private void validateProductionSettings(List<String> errors, List<String> warnings) {
    if (requestsPerMinute > 100) {
      warnings.add("High rate limit in production may impact API stability");
    }

    if (circuitBreakerTimeoutMs < 10000) {
      warnings.add("Short circuit breaker timeout in production may cause unnecessary failures");
    }

    if (!healthCheckEnabled) {
      warnings.add("Health checks should be enabled in production");
    }
  }

  private void validateDevelopmentSettings(List<String> warnings) {
    if (requestsPerMinute > 200) {
      warnings.add("Very high rate limit in development");
    }
  }

  private void validateTestSettings(List<String> warnings) {
    if (circuitBreakerTimeoutMs > 5000) {
      warnings.add("Long circuit breaker timeout may slow down tests");
    }
  }

  private void reportValidationResults(List<String> errors, List<String> warnings) {
    if (!errors.isEmpty()) {
      serviceLogger.logServiceFailure("Configuration Validation",
          "Configuration errors found: " + String.join(", ", errors));
      throw new ConfigurationException("validation", "errors",
          "Configuration errors: " + String.join("; ", errors));
    }

    warnings.forEach(
        warning -> serviceLogger.logServiceWarning("Configuration warning: " + warning));
  }

  private ValidationResult validateUrl(String key, String url) {
    try {
      new URL(url);
      if (!url.startsWith("https://")) {
        return ValidationResult.warning("Should use HTTPS for security");
      }
      return ValidationResult.success("Valid URL");
    } catch (MalformedURLException e) {
      return ValidationResult.error("Invalid URL format");
    }
  }

  private ValidationResult validateTimeout(String key, Long timeout) {
    if (timeout < 1000) {
      return ValidationResult.error("Timeout too low (minimum: 1000ms)");
    } else if (timeout > 300000) {
      return ValidationResult.warning("Timeout very high (recommended: < 300000ms)");
    }
    return ValidationResult.success("Valid timeout");
  }

  private ValidationResult validateRateLimit(String key, Integer value, int min, int max) {
    if (value < min) {
      return ValidationResult.error("Value too low (minimum: " + min + ")");
    } else if (value > max) {
      return ValidationResult.warning("Value very high (maximum recommended: " + max + ")");
    }
    return ValidationResult.success("Valid rate limit");
  }

  // Configuration getters

  private Map<String, Object> getApiConfiguration() {
    return Map.of("baseUrl", openDotaBaseUrl, "readTimeoutMs", readTimeoutMs, "connectTimeoutMs",
        connectTimeoutMs);
  }

  private Map<String, Object> getRateLimitConfiguration() {
    return Map.of("requestsPerMinute", requestsPerMinute, "requestsPerDay", requestsPerDay,
        "burstCapacity",
        burstCapacity, "enableWaiting", enableWaiting);
  }

  private Map<String, Object> getCircuitBreakerConfiguration() {
    return Map.of("failureThreshold", circuitBreakerFailureThreshold, "successThreshold",
        circuitBreakerSuccessThreshold, "timeoutMs", circuitBreakerTimeoutMs, "minimumCalls",
        minimumCalls,
        "slidingWindowSize", slidingWindowSize);
  }

  private Map<String, Object> getHealthCheckConfiguration() {
    return Map.of("enabled", healthCheckEnabled);
  }

  // Supporting classes

  public static class ConfigurationSummary {

    private final String activeProfile;

    private final Map<String, Object> apiConfiguration;

    private final Map<String, Object> rateLimitConfiguration;

    private final Map<String, Object> circuitBreakerConfiguration;

    private final Map<String, Object> healthCheckConfiguration;

    private ConfigurationSummary(Builder builder) {
      this.activeProfile = builder.activeProfile;
      this.apiConfiguration = builder.apiConfiguration;
      this.rateLimitConfiguration = builder.rateLimitConfiguration;
      this.circuitBreakerConfiguration = builder.circuitBreakerConfiguration;
      this.healthCheckConfiguration = builder.healthCheckConfiguration;
    }

    public static Builder builder() {
      return new Builder();
    }

    // Getters
    public String getActiveProfile() {
      return activeProfile;
    }

    public Map<String, Object> getApiConfiguration() {
      return apiConfiguration;
    }

    public Map<String, Object> getRateLimitConfiguration() {
      return rateLimitConfiguration;
    }

    public Map<String, Object> getCircuitBreakerConfiguration() {
      return circuitBreakerConfiguration;
    }

    public Map<String, Object> getHealthCheckConfiguration() {
      return healthCheckConfiguration;
    }

    public static class Builder {

      private String activeProfile;

      private Map<String, Object> apiConfiguration;

      private Map<String, Object> rateLimitConfiguration;

      private Map<String, Object> circuitBreakerConfiguration;

      private Map<String, Object> healthCheckConfiguration;

      public Builder activeProfile(String activeProfile) {
        this.activeProfile = activeProfile;
        return this;
      }

      public Builder apiConfiguration(Map<String, Object> apiConfiguration) {
        this.apiConfiguration = apiConfiguration;
        return this;
      }

      public Builder rateLimitConfiguration(Map<String, Object> rateLimitConfiguration) {
        this.rateLimitConfiguration = rateLimitConfiguration;
        return this;
      }

      public Builder circuitBreakerConfiguration(Map<String, Object> circuitBreakerConfiguration) {
        this.circuitBreakerConfiguration = circuitBreakerConfiguration;
        return this;
      }

      public Builder healthCheckConfiguration(Map<String, Object> healthCheckConfiguration) {
        this.healthCheckConfiguration = healthCheckConfiguration;
        return this;
      }

      public ConfigurationSummary build() {
        return new ConfigurationSummary(this);
      }

    }

  }

  public static class ValidationResult {

    private final boolean valid;

    private final String level; // SUCCESS, WARNING, ERROR

    private final String message;

    private ValidationResult(boolean valid, String level, String message) {
      this.valid = valid;
      this.level = level;
      this.message = message;
    }

    public static ValidationResult success(String message) {
      return new ValidationResult(true, "SUCCESS", message);
    }

    public static ValidationResult warning(String message) {
      return new ValidationResult(true, "WARNING", message);
    }

    public static ValidationResult error(String message) {
      return new ValidationResult(false, "ERROR", message);
    }

    // Getters
    public boolean isValid() {
      return valid;
    }

    public String getLevel() {
      return level;
    }

    public String getMessage() {
      return message;
    }

  }

}