package com.abe.gg_stats.service;

import com.abe.gg_stats.exception.CircuitBreakerException;
import com.abe.gg_stats.util.LoggingUtils;
import com.abe.gg_stats.util.MDCLoggingContext;
import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import lombok.Builder;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Circuit breaker service implementing the Circuit Breaker pattern for preventing
 * cascading failures and providing system resilience.
 * <p>
 * Features: - Configurable failure thresholds and timeouts - Half-open state for gradual
 * recovery testing - Per-service circuit breakers with shared configuration -
 * Comprehensive metrics and monitoring - Thread-safe operations - Graceful degradation
 * support
 */
@Service
@RequiredArgsConstructor
public class CircuitBreakerService {

	private final ServiceLogger serviceLogger;

	private final Map<String, CircuitBreaker> circuitBreakers = new ConcurrentHashMap<>();

	@Value("${circuit-breaker.failure-threshold:20}")
	private int defaultFailureThreshold;

	@Value("${circuit-breaker.success-threshold:3}")
	private int defaultSuccessThreshold;

	/**
	 * Duration after the last recorded failure during which the circuit breaker remains
	 * in the OPEN state. Once this time has passed, it transitions to HALF_OPEN.
	 */
	@Value("${circuit-breaker.timeout-duration-ms:30000}")
	private long defaultTimeoutDurationMs;

	@Value("${circuit-breaker.minimum-calls:10}")
	private int defaultMinimumCalls; // minimum success calls to close the circuit

	@Value("${circuit-breaker.sliding-window-size:100}")
	private int defaultSlidingWindowSize;

	@PostConstruct
	public void initialize() {
		serviceLogger.logServiceSuccess("CircuitBreakerService", "Circuit breaker service initialized",
				"defaultFailureThreshold=" + defaultFailureThreshold,
				"defaultTimeoutDurationMs=" + defaultTimeoutDurationMs,
				"defaultSlidingWindowSize=" + defaultSlidingWindowSize);
	}

	/**
	 * Executes a supplier within a circuit breaker context
	 * @param serviceName The name of the service being protected
	 * @param supplier The operation to execute
	 * @param fallback Optional fallback operation if circuit is open
	 * @return The result of the operation or fallback
	 */
	public <T> T executeWithCircuitBreaker(@NonNull String serviceName, @NonNull Supplier<T> supplier,
			@NonNull Supplier<T> fallback) {
		CircuitBreaker circuitBreaker = getOrCreateCircuitBreaker(serviceName);

		// Preserve existing MDC context for circuit breaker operations
		String existingCorrelationId = MDCLoggingContext.getCurrentCorrelationId();
		String existingBatchType = MDCLoggingContext.getCurrentBatchType();
		String existingOperationType = MDCLoggingContext.getCurrentOperationType();

		if (!circuitBreaker.canExecute()) {
			LoggingUtils.logWarning("Circuit breaker is open, using fallback", "service=" + serviceName,
					"state=" + circuitBreaker.getState());
			return fallback.get();
		}

		try (LoggingUtils.AutoCloseableStopWatch _ = LoggingUtils.createStopWatch("circuit_breaker_" + serviceName)) {

			T result = supplier.get();
			circuitBreaker.recordSuccess();
			return result;

		}
		catch (Exception e) {
			circuitBreaker.recordFailure();
			serviceLogger.logServiceFailure("circuit_breaker_execution", "Unexpected exception occurred ", e);
			throw new CircuitBreakerException(serviceName, "Closed", "Operation failed for service");
		}
	}

	/**
	 * Manually opens a circuit breaker
	 */
	public void openCircuitBreaker(String serviceName, String reason) {
		CircuitBreaker circuitBreaker = getOrCreateCircuitBreaker(serviceName);
		circuitBreaker.forceOpen(reason);
		LoggingUtils.logWarning("Circuit breaker manually opened", "service=" + serviceName, "reason=" + reason);
	}

	/**
	 * Manually closes a circuit breaker
	 */
	public void closeCircuitBreaker(String serviceName) {
		CircuitBreaker circuitBreaker = circuitBreakers.get(serviceName);
		if (circuitBreaker != null) {
			circuitBreaker.forceClose();
			serviceLogger.logServiceSuccess("CircuitBreakerService", "Circuit breaker manually closed",
					"service=" + serviceName);
		}
	}

	/**
	 * Gets the current state of a circuit breaker
	 */
	public CircuitBreakerState getState(String serviceName) {
		CircuitBreaker circuitBreaker = circuitBreakers.get(serviceName);
		return circuitBreaker != null ? circuitBreaker.getState() : CircuitBreakerState.CLOSED;
	}

	/**
	 * Gets comprehensive status for all circuit breakers
	 */
	public Map<String, CircuitBreakerStatus> getAllStatuses() {
		Map<String, CircuitBreakerStatus> statuses = new ConcurrentHashMap<>();

		circuitBreakers.forEach((serviceName, circuitBreaker) -> {
			statuses.put(serviceName, circuitBreaker.getStatus());
		});

		return statuses;
	}

	/**
	 * Gets status for a specific circuit breaker
	 */
	public CircuitBreakerStatus getStatus(String serviceName) {
		CircuitBreaker circuitBreaker = circuitBreakers.get(serviceName);
		return circuitBreaker != null ? circuitBreaker.getStatus() : createDefaultStatus(serviceName);
	}

	/**
	 * Resets all metrics for a circuit breaker
	 */
	public void resetMetrics(String serviceName) {
		CircuitBreaker circuitBreaker = circuitBreakers.get(serviceName);
		if (circuitBreaker != null) {
			circuitBreaker.resetMetrics();
			serviceLogger.logServiceSuccess("CircuitBreakerService", "Circuit breaker metrics reset",
					"service=" + serviceName);
		}
	}

	// Private helper methods

	private CircuitBreaker getOrCreateCircuitBreaker(String serviceName) {
		return circuitBreakers.computeIfAbsent(serviceName, key -> new CircuitBreaker(key, createDefaultConfig()));
	}

	private CircuitBreakerConfig createDefaultConfig() {
		return CircuitBreakerConfig.builder()
			.failureThreshold(defaultFailureThreshold)
			.successThreshold(defaultSuccessThreshold)
			.timeoutDuration(Duration.ofMillis(defaultTimeoutDurationMs))
			.minimumCalls(defaultMinimumCalls)
			.slidingWindowSize(defaultSlidingWindowSize)
			.build();
	}

	private CircuitBreakerStatus createDefaultStatus(String serviceName) {
		return CircuitBreakerStatus.builder()
			.serviceName(serviceName)
			.state(CircuitBreakerState.CLOSED)
			.totalCalls(0)
			.successCalls(0)
			.failureCalls(0)
			.successRate(100.0)
			.lastStateChangeTime(Instant.now())
			.build();
	}

	public enum CircuitBreakerState {

		CLOSED, // Normal operation
		OPEN, // Blocking calls due to failures
		HALF_OPEN // Testing if service has recovered

	}

	/**
	 * Circuit breaker implementation with sliding window failure tracking
	 */
	private static class CircuitBreaker {

		private final String serviceName;

		private final CircuitBreakerConfig config;

		private final AtomicReference<CircuitBreakerState> state;

		private final AtomicLong lastStateChangeTime;

		private final AtomicInteger consecutiveSuccesses;

		private final SlidingWindow slidingWindow;

		private final AtomicLong totalCalls;

		private final AtomicLong successCalls;

		private final AtomicLong failureCalls;

		private volatile String lastFailureReason;

		public CircuitBreaker(String serviceName, CircuitBreakerConfig config) {
			this.serviceName = serviceName;
			this.config = config;
			this.state = new AtomicReference<>(CircuitBreakerState.CLOSED);
			this.lastStateChangeTime = new AtomicLong(System.currentTimeMillis());
			this.consecutiveSuccesses = new AtomicInteger(0);
			this.slidingWindow = new SlidingWindow(config.slidingWindowSize());
			this.totalCalls = new AtomicLong(0);
			this.successCalls = new AtomicLong(0);
			this.failureCalls = new AtomicLong(0);
		}

		public boolean canExecute() {
			CircuitBreakerState currentState = state.get();

			return switch (currentState) {
				case CLOSED, HALF_OPEN -> true;
				case OPEN -> shouldTransitionToHalfOpen();
			};
		}

		public void recordSuccess() {
			totalCalls.incrementAndGet();
			successCalls.incrementAndGet();
			slidingWindow.recordSuccess();

			CircuitBreakerState currentState = state.get();

			if (currentState == CircuitBreakerState.HALF_OPEN) {
				int successes = consecutiveSuccesses.incrementAndGet();
				if (successes >= config.successThreshold()) {
					transitionToState(CircuitBreakerState.CLOSED, "Success threshold reached");
					consecutiveSuccesses.set(0);
				}
			}
			else if (currentState == CircuitBreakerState.OPEN) {
				// Reset consecutive successes when transitioning from open
				consecutiveSuccesses.set(1);
			}
		}

		public void recordFailure() {
			totalCalls.incrementAndGet();
			failureCalls.incrementAndGet();
			slidingWindow.recordFailure();
			consecutiveSuccesses.set(0);

			CircuitBreakerState currentState = state.get();

			if (currentState == CircuitBreakerState.HALF_OPEN) {
				transitionToState(CircuitBreakerState.OPEN, "Failure in half-open state");
			}
			else if (currentState == CircuitBreakerState.CLOSED) {
				if (shouldOpen()) {
					transitionToState(CircuitBreakerState.OPEN, "Failure threshold exceeded");
				}
			}
		}

		public void forceOpen(String reason) {
			transitionToState(CircuitBreakerState.OPEN, "Manually opened: " + reason);
			lastFailureReason = reason;
		}

		public void forceClose() {
			transitionToState(CircuitBreakerState.CLOSED, "Manually closed");
			consecutiveSuccesses.set(0);
		}

		public CircuitBreakerState getState() {
			return state.get();
		}

		public CircuitBreakerStatus getStatus() {
			long total = totalCalls.get();
			double successRate = total > 0 ? (double) successCalls.get() / total * 100.0 : 100.0;

			return CircuitBreakerStatus.builder()
				.serviceName(serviceName)
				.state(state.get())
				.totalCalls(total)
				.successCalls(successCalls.get())
				.failureCalls(failureCalls.get())
				.successRate(successRate)
				.consecutiveSuccesses(consecutiveSuccesses.get())
				.lastStateChangeTime(Instant.ofEpochMilli(lastStateChangeTime.get()))
				.slidingWindowFailureRate(slidingWindow.getFailureRate())
				.lastFailureReason(lastFailureReason)
				.build();
		}

		public void resetMetrics() {
			totalCalls.set(0);
			successCalls.set(0);
			failureCalls.set(0);
			consecutiveSuccesses.set(0);
			slidingWindow.reset();
			lastFailureReason = null;
		}

		private boolean shouldTransitionToHalfOpen() {
			long timeSinceLastStateChange = System.currentTimeMillis() - lastStateChangeTime.get();

			if (timeSinceLastStateChange >= config.timeoutDuration().toMillis()) {
				transitionToState(CircuitBreakerState.HALF_OPEN, "Timeout period elapsed");
				return true;
			}

			return false;
		}

		private boolean shouldOpen() {
			if (totalCalls.get() < config.minimumCalls()) {
				return false; // Not enough calls to make a decision
			}

			double failureRate = slidingWindow.getFailureRate();
			int failureThreshold = config.failureThreshold();

			// Convert threshold to percentage if it's provided as count
			double thresholdPercentage = failureThreshold <= 100 ? failureThreshold
					: (double) failureThreshold / config.slidingWindowSize() * 100;

			return failureRate >= thresholdPercentage;
		}

		private void transitionToState(CircuitBreakerState newState, String reason) {
			CircuitBreakerState oldState = state.getAndSet(newState);
			lastStateChangeTime.set(System.currentTimeMillis());
		}

	}

	/**
	 * Sliding window for tracking recent call results
	 */
	private static class SlidingWindow {

		private final boolean[] results;

		private final AtomicInteger index;

		private final AtomicInteger size;

		private final int capacity;

		public SlidingWindow(int capacity) {
			this.capacity = capacity;
			this.results = new boolean[capacity];
			this.index = new AtomicInteger(0);
			this.size = new AtomicInteger(0);
		}

		public synchronized void recordSuccess() {
			results[index.get() % capacity] = true;
			index.incrementAndGet();
			if (size.get() < capacity) {
				size.incrementAndGet();
			}
		}

		public synchronized void recordFailure() {
			results[index.get() % capacity] = false;
			index.incrementAndGet();
			if (size.get() < capacity) {
				size.incrementAndGet();
			}
		}

		public synchronized double getFailureRate() {
			if (size.get() == 0) {
				return 0.0;
			}

			int failures = 0;
			int currentSize = size.get();

			for (int i = 0; i < currentSize; i++) {
				if (!results[i]) {
					failures++;
				}
			}

			return (double) failures / currentSize * 100.0;
		}

		public synchronized void reset() {
			index.set(0);
			size.set(0);
			// No need to clear the array, it will be overwritten
		}

	}

	/**
	 * Circuit breaker configuration
	 */
	@Builder
	public record CircuitBreakerConfig(int failureThreshold, int successThreshold, Duration timeoutDuration,
			int minimumCalls, int slidingWindowSize) {

	}

	/**
	 * Circuit breaker status for monitoring
	 *
	 * @param serviceName Getters
	 */
	@Builder
	public record CircuitBreakerStatus(@NonNull String serviceName, @NonNull CircuitBreakerState state, long totalCalls,
			long successCalls, long failureCalls, double successRate, int consecutiveSuccesses,
			Instant lastStateChangeTime, double slidingWindowFailureRate, String lastFailureReason) {

	}

}