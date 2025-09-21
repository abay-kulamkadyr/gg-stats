package com.abe.gg_stats.service.circuit_breaker;

import static org.junit.jupiter.api.Assertions.*;

import com.abe.gg_stats.exception.CircuitBreakerException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.atomic.AtomicInteger;

class CircuitBreakerServiceTest {

	private CircuitBreakerService service;

	@BeforeEach
	void setUp() {
		service = new CircuitBreakerService();
		// Make thresholds small so transitions happen quickly
		ReflectionTestUtils.setField(service, "defaultFailureThreshold", 2);
		ReflectionTestUtils.setField(service, "defaultSuccessThreshold", 2);
		ReflectionTestUtils.setField(service, "defaultTimeoutDurationMs", 50L);
		ReflectionTestUtils.setField(service, "defaultMinimumCalls", 1);
		ReflectionTestUtils.setField(service, "defaultSlidingWindowSize", 10);
	}

	@Test
	void transitionsClosedToOpenOnFailuresAndHalfOpenAfterTimeout() throws InterruptedException {
		String name = "api";
		// First failure should throw and likely OPEN the breaker (low thresholds)
		assertThrows(CircuitBreakerException.class, () -> service.executeWithCircuitBreaker(name, () -> {
			throw new RuntimeException("boom");
		}, () -> null));

		// Once OPEN, subsequent calls use fallback and should NOT throw
		assertDoesNotThrow(() -> service.executeWithCircuitBreaker(name, () -> {
			throw new RuntimeException("boom");
		}, () -> null));

		assertEquals(CircuitBreakerService.CircuitBreakerState.OPEN, service.getState(name));

		// Wait for timeout to allow HALF_OPEN
		Thread.sleep(60);
		// Trigger canExecute by invoking the breaker
		assertDoesNotThrow(() -> service.executeWithCircuitBreaker(name, () -> "ok", () -> null));
		assertEquals(CircuitBreakerService.CircuitBreakerState.HALF_OPEN, service.getState(name));
	}

	@Test
	void closesAfterEnoughSuccessInHalfOpen() throws InterruptedException {
		String name = "close-after-success";
		// Trip open
		try {
			service.executeWithCircuitBreaker(name, () -> {
				throw new RuntimeException();
			}, () -> null);
		}
		catch (Exception ignored) {
		}
		try {
			service.executeWithCircuitBreaker(name, () -> {
				throw new RuntimeException();
			}, () -> null);
		}
		catch (Exception ignored) {
		}
		assertEquals(CircuitBreakerService.CircuitBreakerState.OPEN, service.getState(name));

		// Wait to transition to HALF_OPEN, then trigger transition by calling execute
		Thread.sleep(60);
		assertDoesNotThrow(() -> service.executeWithCircuitBreaker(name, () -> "ok", () -> null));
		assertEquals(CircuitBreakerService.CircuitBreakerState.HALF_OPEN, service.getState(name));

		// Provide one more success (total 2) to close
		assertDoesNotThrow(() -> service.executeWithCircuitBreaker(name, () -> "ok", () -> null));
		assertEquals(CircuitBreakerService.CircuitBreakerState.CLOSED, service.getState(name));
	}

	@Test
	void executeFallbackWhenOpen() {
		String name = "fallback";
		// Force open
		service.openCircuitBreaker(name, "test");
		String result = service.executeWithCircuitBreaker(name, () -> "primary", () -> "fallback");
		assertEquals("fallback", result);
	}

}
