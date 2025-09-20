package com.abe.gg_stats.service.circuit_breaker;

import com.abe.gg_stats.exception.CircuitBreakerException;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class CircuitBreakerService {

	private final Map<String, CircuitBreaker> circuitBreakers = new ConcurrentHashMap<>();

	@Value("${circuit-breaker.failure-threshold:20}")
	private int defaultFailureThreshold;

	@Value("${circuit-breaker.success-threshold:3}")
	private int defaultSuccessThreshold;

	@Value("${circuit-breaker.timeout-duration-ms:30000}")
	private long defaultTimeoutDurationMs;

	@Value("${circuit-breaker.minimum-calls:10}")
	private int defaultMinimumCalls;

	@Value("${circuit-breaker.sliding-window-size:100}")
	private int defaultSlidingWindowSize;

	public <T> T executeWithCircuitBreaker(@NonNull String serviceName, @NonNull Supplier<T> supplier,
			@NonNull Supplier<T> fallback) {
		CircuitBreaker circuitBreaker = getOrCreateCircuitBreaker(serviceName);

		if (!circuitBreaker.canExecute()) {
			log.warn("Circuit breaker [{}] is OPEN. Executing fallback.", serviceName);
			return fallback.get();
		}

		try {
			T result = supplier.get();
			circuitBreaker.recordSuccess();
			return result;
		}
		catch (Exception e) {
			circuitBreaker.recordFailure();
			log.error("Operation failed for service [{}]. State: [{}]. Error: {}", serviceName,
					circuitBreaker.getState(), e.toString());
			throw new CircuitBreakerException(serviceName, circuitBreaker.getState().name(),
					"Operation failed for service");
		}
	}

	public void openCircuitBreaker(String serviceName, String reason) {
		getOrCreateCircuitBreaker(serviceName).forceOpen(reason);
		log.warn("Circuit breaker [{}] forced OPEN. Reason: {}", serviceName, reason);
	}

	public void closeCircuitBreaker(String serviceName) {
		CircuitBreaker cb = circuitBreakers.get(serviceName);
		if (cb != null) {
			cb.forceClose();
			log.info("Circuit breaker [{}] forced CLOSED.", serviceName);
		}
	}

	public CircuitBreakerState getState(String serviceName) {
		return circuitBreakers.getOrDefault(serviceName, null) != null ? circuitBreakers.get(serviceName).getState()
				: CircuitBreakerState.CLOSED;
	}

	public Map<String, CircuitBreakerStatus> getAllStatuses() {
		Map<String, CircuitBreakerStatus> statuses = new ConcurrentHashMap<>();
		circuitBreakers.forEach((serviceName, breaker) -> statuses.put(serviceName, breaker.getStatus()));
		return statuses;
	}

	public CircuitBreakerStatus getStatus(String serviceName) {
		return circuitBreakers.containsKey(serviceName) ? circuitBreakers.get(serviceName).getStatus()
				: createDefaultStatus(serviceName);
	}

	public void resetMetrics(String serviceName) {
		CircuitBreaker cb = circuitBreakers.get(serviceName);
		if (cb != null) {
			cb.resetMetrics();
			log.info("Metrics reset for circuit breaker [{}].", serviceName);
		}
	}

	// Helpers
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

	// ========= Inner Classes =========

	public enum CircuitBreakerState {

		CLOSED, OPEN, HALF_OPEN

	}

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

		CircuitBreaker(String serviceName, CircuitBreakerConfig config) {
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

		boolean canExecute() {
			return switch (state.get()) {
				case CLOSED, HALF_OPEN -> true;
				case OPEN -> shouldTransitionToHalfOpen();
			};
		}

		void recordSuccess() {
			totalCalls.incrementAndGet();
			successCalls.incrementAndGet();
			slidingWindow.recordSuccess();

			if (state.get() == CircuitBreakerState.HALF_OPEN) {
				if (consecutiveSuccesses.incrementAndGet() >= config.successThreshold()) {
					transitionToState(CircuitBreakerState.CLOSED);
					log.info("Circuit breaker [{}] transitioned to CLOSED after successful calls.", serviceName);
					consecutiveSuccesses.set(0);
				}
			}
		}

		void recordFailure() {
			totalCalls.incrementAndGet();
			failureCalls.incrementAndGet();
			slidingWindow.recordFailure();
			consecutiveSuccesses.set(0);

			if (state.get() == CircuitBreakerState.HALF_OPEN) {
				transitionToState(CircuitBreakerState.OPEN);
				log.warn("Circuit breaker [{}] transitioned back to OPEN after failure in HALF_OPEN state.",
						serviceName);
			}
			else if (state.get() == CircuitBreakerState.CLOSED && shouldOpen()) {
				transitionToState(CircuitBreakerState.OPEN);
				log.warn("Circuit breaker [{}] transitioned to OPEN due to failure threshold reached.", serviceName);
			}
		}

		void forceOpen(String reason) {
			transitionToState(CircuitBreakerState.OPEN);
			lastFailureReason = reason;
		}

		void forceClose() {
			transitionToState(CircuitBreakerState.CLOSED);
			consecutiveSuccesses.set(0);
		}

		CircuitBreakerState getState() {
			return state.get();
		}

		CircuitBreakerStatus getStatus() {
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

		void resetMetrics() {
			totalCalls.set(0);
			successCalls.set(0);
			failureCalls.set(0);
			consecutiveSuccesses.set(0);
			slidingWindow.reset();
			lastFailureReason = null;
		}

		private boolean shouldTransitionToHalfOpen() {
			long elapsed = System.currentTimeMillis() - lastStateChangeTime.get();
			if (elapsed >= config.timeoutDuration().toMillis()) {
				transitionToState(CircuitBreakerState.HALF_OPEN);
				log.info("Circuit breaker [{}] transitioned to HALF_OPEN (timeout elapsed).", serviceName);
				return true;
			}
			return false;
		}

		private boolean shouldOpen() {
			if (totalCalls.get() < config.minimumCalls())
				return false;

			double failureRate = slidingWindow.getFailureRate();
			int threshold = config.failureThreshold();
			double thresholdPercent = threshold <= 100 ? threshold
					: (double) threshold / config.slidingWindowSize() * 100;

			return failureRate >= thresholdPercent;
		}

		private void transitionToState(CircuitBreakerState newState) {
			state.set(newState);
			lastStateChangeTime.set(System.currentTimeMillis());
		}

	}

	// Sliding window unchanged
	private static class SlidingWindow {

		private final boolean[] results;

		private final AtomicInteger index = new AtomicInteger(0);

		private final AtomicInteger size = new AtomicInteger(0);

		private final int capacity;

		SlidingWindow(int capacity) {
			this.capacity = capacity;
			this.results = new boolean[capacity];
		}

		synchronized void recordSuccess() {
			results[index.getAndIncrement() % capacity] = true;
			if (size.get() < capacity)
				size.incrementAndGet();
		}

		synchronized void recordFailure() {
			results[index.getAndIncrement() % capacity] = false;
			if (size.get() < capacity)
				size.incrementAndGet();
		}

		synchronized double getFailureRate() {
			int currentSize = size.get();
			if (currentSize == 0)
				return 0.0;

			int failures = 0;
			for (int i = 0; i < currentSize; i++) {
				if (!results[i])
					failures++;
			}
			return (double) failures / currentSize * 100.0;
		}

		synchronized void reset() {
			index.set(0);
			size.set(0);
		}

	}

	@Builder
	public record CircuitBreakerConfig(int failureThreshold, int successThreshold, Duration timeoutDuration,
			int minimumCalls, int slidingWindowSize) {
	}

	@Builder
	public record CircuitBreakerStatus(@NonNull String serviceName, @NonNull CircuitBreakerState state, long totalCalls,
			long successCalls, long failureCalls, double successRate, int consecutiveSuccesses,
			Instant lastStateChangeTime, double slidingWindowFailureRate, String lastFailureReason) {
	}

}
