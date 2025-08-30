package com.abe.gg_stats.service;

import com.abe.gg_stats.entity.ApiRateLimit;
import com.abe.gg_stats.repository.ApiRateLimitRepository;
import com.abe.gg_stats.util.LoggingUtils;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Rate limiting service using token bucket algorithm with database persistence for
 * distributed environments.
 * <p>
 * Features: - Token bucket rate limiting for smooth request distribution - Database
 * persistence for rate limit state - Thread-safe operations with minimal contention -
 * Configurable rate limits per endpoint - Metrics collection for monitoring - Graceful
 * degradation under load
 */
@Service
@RequiredArgsConstructor
public class RateLimitingService {

	private static final String GLOBAL_TRACKING_ENDPOINT = "GLOBAL";

	private static final long DATABASE_SYNC_INTERVAL_MS = 30000; // 30 seconds

	private final ApiRateLimitRepository rateLimitRepository;

	private final ServiceLogger serviceLogger;

	// Single global token bucket for rate limiting
	private volatile TokenBucket globalTokenBucket;

	// Database state management
	private volatile ApiRateLimit globalRateLimit;

	private final AtomicLong lastDatabaseSync = new AtomicLong(0);

	// Background task executor
	private ScheduledExecutorService scheduler;

	// Metrics
	private final AtomicLong totalRequests = new AtomicLong(0);

	private final AtomicLong rejectedRequests = new AtomicLong(0);

	@Value("${opendota.api.rate-limit.per-minute:50}")
	private int requestsPerMinute;

	@Value("${opendota.api.rate-limit.per-day:1800}")
	private int requestsPerDay;

	@Value("${opendota.api.refill-interval.milliseconds:60000}")
	private long refillInterval;

	@PostConstruct
	public void initialize() {
		serviceLogger.logServiceStart("RateLimitingService", "Rate limiting service initialization");

		// Initialize scheduler for background tasks
		scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
			Thread t = new Thread(r, "RateLimit-DatabaseSync");
			t.setDaemon(true);
			return t;
		});

		// Load or create global rate limit from database
		initializeGlobalRateLimit();

		// Create global token bucket
		globalTokenBucket = new TokenBucket(GLOBAL_TRACKING_ENDPOINT, requestsPerMinute, refillInterval);

		// Schedule database sync
		scheduler.scheduleWithFixedDelay(this::saveToDatabaseAsync, DATABASE_SYNC_INTERVAL_MS,
				DATABASE_SYNC_INTERVAL_MS, TimeUnit.MILLISECONDS);

		serviceLogger.logServiceSuccess("RateLimitingService", "Rate limiting service initialized",
				"requestsPerMinute=" + requestsPerMinute, "requestsPerDay=" + requestsPerDay);
	}

	@PreDestroy
	public void cleanup() {
		if (scheduler != null && !scheduler.isShutdown()) {
			scheduler.shutdown();
			try {
				if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
					scheduler.shutdownNow();
				}
			}
			catch (InterruptedException e) {
				scheduler.shutdownNow();
				Thread.currentThread().interrupt();
			}
		}
		// Final save to database
		saveToDatabaseAsync();
	}

	/**
	 * Attempts to acquire a permit for making an API request. Will block and wait if
	 * necessary to respect external API rate limits. Designed for batch job usage where
	 * blocking is acceptable.
	 * @param endpoint The API endpoint requesting access
	 * @return RateLimitResult indicating success/failure and wait time
	 */
	public RateLimitResult tryAcquirePermit(String endpoint) {
		totalRequests.incrementAndGet();

		try (LoggingUtils.AutoCloseableStopWatch _ = LoggingUtils.createStopWatch("rate_limit_check")) {

			// Check daily limit first (fast check)
			if (isDailyLimitExceeded()) {
				rejectedRequests.incrementAndGet();
				return RateLimitResult.rejected("Daily limit exceeded", getRemainingDailyRequests(),
						getTimeUntilDailyReset());
			}

			// Try to acquire from token bucket
			TokenBucket bucket = globalTokenBucket;
			if (bucket == null) {
				rejectedRequests.incrementAndGet();
				return RateLimitResult.rejected("Service not initialized", 0, 0);
			}

			// First try immediate acquisition
			if (bucket.tryAcquire()) {
				recordSuccessfulRequest();
				return RateLimitResult.success();
			}

			// If no tokens available, wait for next refill
			// This is correct for batch jobs calling external APIs with rate limits
			try {
				long waitTime = bucket.getTimeUntilNextToken();

				LoggingUtils.logDebug("Waiting for rate limit token", () -> "endpoint=" + endpoint,
						() -> "waitTime=" + waitTime + "ms");

				boolean acquired = bucket.tryAcquireWithWait(waitTime);

				if (acquired) {
					recordSuccessfulRequest();
					return RateLimitResult.success(waitTime);
				}
				else {
					rejectedRequests.incrementAndGet();
					return RateLimitResult.rejected("Timeout waiting for rate limit", bucket.getAvailableTokens(),
							bucket.getTimeUntilNextToken());
				}

			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				rejectedRequests.incrementAndGet();
				return RateLimitResult.rejected("Interrupted while waiting", bucket.getAvailableTokens(), 0);
			}
		}
	}

	/**
	 * Records a successful API request for metrics and persistence
	 */
	private void recordSuccessfulRequest() {
		try {
			ApiRateLimit current = this.globalRateLimit;
			if (current == null)
				return;

			// Update in-memory state
			LocalDate today = getCurrentLocalDateUTC();
			if (!current.getDailyWindowStart().equals(today)) {
				// New day - reset counter
				current.setDailyWindowStart(today);
				current.setDailyRequests(1);
			}
			else {
				current.setDailyRequests(current.getDailyRequests() + 1);
			}
		}
		catch (Exception e) {
			serviceLogger.logServiceFailure("record_successful_request", "Error updating rate limit", e);
		}
	}

	/**
	 * Gets current rate limiting status and metrics
	 */
	public RateLimitStatus getStatus() {
		TokenBucket bucket = globalTokenBucket;

		return RateLimitStatus.builder()
			.availableTokens(bucket != null ? bucket.getAvailableTokens() : 0)
			.remainingDailyRequests(getRemainingDailyRequests())
			.totalRequests(totalRequests.get())
			.rejectedRequests(rejectedRequests.get())
			.successRate(calculateSuccessRate())
			.timeUntilDailyReset(getTimeUntilDailyReset())
			.build();
	}

	private void initializeGlobalRateLimit() {
		try {
			this.globalRateLimit = rateLimitRepository.findByEndpoint(GLOBAL_TRACKING_ENDPOINT)
				.orElseGet(this::createNewGlobalRateLimit);
		}
		catch (Exception e) {
			serviceLogger.logServiceFailure("initialize_global_rate_limit", "Database error", e);
			this.globalRateLimit = createNewGlobalRateLimit();
		}
	}

	@Transactional
	public void saveToDatabaseAsync() {
		try {
			ApiRateLimit current = this.globalRateLimit;
			if (current != null) {
				// Find the managed entity and update it
				ApiRateLimit managed = rateLimitRepository.findByEndpoint(GLOBAL_TRACKING_ENDPOINT).orElse(current);

				managed.setDailyRequests(current.getDailyRequests());
				managed.setDailyWindowStart(current.getDailyWindowStart());
				managed.setWindowStart(getCurrentDateTimeUTC());

				rateLimitRepository.save(managed);
				LoggingUtils.logDebug("Rate limit state saved to database");
			}
		}
		catch (Exception e) {
			serviceLogger.logServiceFailure("save_to_database", "Database sync error", e);
		}
	}

	private boolean isDailyLimitExceeded() {
		return getRemainingDailyRequests() <= 0;
	}

	private int getRemainingDailyRequests() {
		try {
			ApiRateLimit current = this.globalRateLimit;
			if (current == null)
				return requestsPerDay;

			return Math.max(0, requestsPerDay - current.getDailyRequests());
		}
		catch (Exception e) {
			serviceLogger.logServiceFailure("get_remaining_daily_requests", "Error getting remaining requests", e);
			return requestsPerDay; // Fail open
		}
	}

	private long getTimeUntilDailyReset() {
		ZonedDateTime now = getCurrentDateTimeUTC();
		ZonedDateTime nextDay = now.plusDays(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
		return Duration.between(now, nextDay).toMillis();
	}

	private double calculateSuccessRate() {
		long total = totalRequests.get();
		if (total == 0)
			return 100.0;

		long successful = total - rejectedRequests.get();
		return (double) successful / total * 100.0;
	}

	private ApiRateLimit createNewGlobalRateLimit() {
		ZonedDateTime now = getCurrentDateTimeUTC();
		LocalDate today = now.toLocalDate();
		return ApiRateLimit.builder()
			.endpoint(GLOBAL_TRACKING_ENDPOINT)
			.requestsCount(0)
			.windowStart(now)
			.dailyRequests(0)
			.dailyWindowStart(today)
			.build();
	}

	private ZonedDateTime getCurrentDateTimeUTC() {
		return ZonedDateTime.ofInstant(Instant.now(), ZoneId.of("UTC"));
	}

	private LocalDate getCurrentLocalDateUTC() {
		return getCurrentDateTimeUTC().toLocalDate();
	}

	/**
	 * Thread-safe token bucket implementation for smooth rate limiting. Supports blocking
	 * operations for batch job usage.
	 */
	private static class TokenBucket {

		private final String endpoint;

		private final AtomicInteger currentTokens;

		private final AtomicLong lastRefillTime;

		private final int maxTokens;

		private final long refillIntervalMs;

		public TokenBucket(String endpoint, int maxTokens, long refillIntervalMs) {
			this.endpoint = endpoint;
			this.maxTokens = maxTokens;
			this.refillIntervalMs = refillIntervalMs;
			long now = System.currentTimeMillis();
			this.lastRefillTime = new AtomicLong(now);
			this.currentTokens = new AtomicInteger(maxTokens);
		}

		// Immediate acquisition attempt - non-blocking
		public boolean tryAcquire() {
			return tryAcquireToken();
		}

		// Blocking acquisition with timeout
		public boolean tryAcquireWithWait(long timeoutMs) throws InterruptedException {
			Thread.sleep(timeoutMs);
			return tryAcquireToken();
		}

		private boolean tryAcquireToken() {
			while (true) {
				refillTokens();
				int current = currentTokens.get();

				if (current <= 0) {
					return false;
				}

				if (currentTokens.compareAndSet(current, current - 1)) {
					return true;
				}
				// CAS failed, retry
			}
		}

		public int getAvailableTokens() {
			refillTokens();
			return currentTokens.get();
		}

		public long getTimeUntilNextToken() {
			if (getAvailableTokens() > 0) {
				return 0;
			}
			long timeSinceLastRefill = System.currentTimeMillis() - lastRefillTime.get();
			long timeUntilNext = Math.max(0, refillIntervalMs - timeSinceLastRefill);

			LoggingUtils.logDebug("Time until next token available", () -> "endpoint=" + endpoint,
					() -> "waitTime=" + timeUntilNext + "ms");

			return timeUntilNext;
		}

		private void refillTokens() {
			long now = System.currentTimeMillis();
			long lastRefill = lastRefillTime.get();
			long timeSinceLastRefill = now - lastRefill;

			if (timeSinceLastRefill >= refillIntervalMs) {
				if (lastRefillTime.compareAndSet(lastRefill, now)) {
					currentTokens.set(maxTokens);
					LoggingUtils.logDebug("Token bucket refilled", () -> "endpoint=" + endpoint,
							() -> "maxTokens=" + maxTokens);
				}
			}
		}

	}

	/**
	 * Result of a rate limit check
	 */
	@Builder
	public record RateLimitResult(boolean allowed, String reason, long waitTimeMs, int remainingRequests,
			long resetTimeMs) {

		public static RateLimitResult success() {
			return new RateLimitResult(true, null, 0, -1, 0);
		}

		public static RateLimitResult success(long waitTimeMs) {
			return new RateLimitResult(true, null, waitTimeMs, -1, 0);
		}

		public static RateLimitResult rejected(String reason, int remainingRequests, long resetTimeMs) {
			return new RateLimitResult(false, reason, 0, remainingRequests, resetTimeMs);
		}
	}

	/**
	 * Current rate limiting status for monitoring
	 */
	@Builder
	public record RateLimitStatus(int availableTokens, int remainingDailyRequests, long totalRequests,
			long rejectedRequests, double successRate, long timeUntilDailyReset) {
	}

}