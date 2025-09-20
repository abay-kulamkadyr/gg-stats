package com.abe.gg_stats.service.rate_limit;

import com.abe.gg_stats.entity.ApiRateLimit;
import com.abe.gg_stats.exception.RateLimitExceededException;
import com.abe.gg_stats.repository.ApiRateLimitRepository;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Rate limiting service using token bucket algorithm with database persistence for
 * distributed environments.
 * <p>
 * Logging: structured key=value style. Hot-path success messages are TRACE to avoid
 * noise.
 */
@Service
@Slf4j
public class OpenDotaRateLimitingService {

	private static final String GLOBAL_TRACKING_ENDPOINT = "GLOBAL";

	private static final long DATABASE_SYNC_INTERVAL_MS = 30_000L; // 30 seconds

	private static final long MIN_SYNC_INTERVAL_MS = 5_000L; // Minimum time between syncs

	private final ApiRateLimitRepository rateLimitRepository;

	private final AtomicLong lastDatabaseSync = new AtomicLong(0);

	private final AtomicLong lastSuccessfulSync = new AtomicLong(0);

	private final AtomicInteger pendingChanges = new AtomicInteger(0);

	private final ReentrantReadWriteLock stateLock = new ReentrantReadWriteLock();

	// Metrics with better tracking
	private final AtomicLong totalRequests = new AtomicLong(0);

	private final AtomicLong rejectedRequests = new AtomicLong(0);

	private final AtomicLong databaseSyncErrors = new AtomicLong(0);

	private final AtomicLong successfulSyncs = new AtomicLong(0);

	// Single global token bucket for rate limiting
	private volatile TokenBucket globalTokenBucket;

	// Database state management with improved synchronization
	private volatile ApiRateLimit globalRateLimit;

	// Background task executor
	private ScheduledExecutorService scheduler;

	@Value("${opendota.api.rate-limit.per-minute:50}")
	private int requestsPerMinute;

	@Value("${opendota.api.rate-limit.per-day:1800}")
	private int requestsPerDay;

	@Value("${opendota.api.refill-interval.milliseconds:60000}")
	private long refillInterval;

	@Autowired
	public OpenDotaRateLimitingService(ApiRateLimitRepository apiRateLimitRepository) {
		this.rateLimitRepository = apiRateLimitRepository;
	}

	@PostConstruct
	public void initialize() {
		try {
			log.info(
					"service_starting component=OpenDotaRateLimitingService requestsPerMinute={} requestsPerDay={} refillIntervalMs={}",
					requestsPerMinute, requestsPerDay, refillInterval);

			// Initialize scheduler for background tasks
			scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
				Thread t = new Thread(r, "RateLimit-DatabaseSync");
				t.setDaemon(true);
				t.setUncaughtExceptionHandler((thread, ex) -> log
					.error("scheduler_uncaught_exception component=OpenDotaRateLimitingService", ex));
				return t;
			});

			// Load or create global rate limit from database
			initializeGlobalRateLimit();

			// Create global token bucket
			globalTokenBucket = new TokenBucket(requestsPerMinute, refillInterval);

			// Schedule database sync with improved strategy
			scheduler.scheduleWithFixedDelay(this::periodicDatabaseSync, DATABASE_SYNC_INTERVAL_MS,
					DATABASE_SYNC_INTERVAL_MS, TimeUnit.MILLISECONDS);

			// Schedule metrics logging (configurable if desired)
			scheduler.scheduleWithFixedDelay(this::logMetrics, 60_000L, 60_000L, TimeUnit.MILLISECONDS); // Every
																											// minute

			log.info("service_started component=OpenDotaRateLimitingService availableTokens={} remainingDaily={}",
					globalTokenBucket != null ? globalTokenBucket.getAvailableTokens() : 0,
					getRemainingDailyRequests());

		}
		catch (Exception e) {
			log.error("service_start_failure component=OpenDotaRateLimitingService", e);
			throw new IllegalStateException("Rate limiting service initialization failed", e);
		}
	}

	@PreDestroy
	public void cleanup() {
		log.info("service_shutting_down component=OpenDotaRateLimitingService");

		if (scheduler != null && !scheduler.isShutdown()) {
			scheduler.shutdown();
			try {
				if (!scheduler.awaitTermination(10, TimeUnit.SECONDS)) {
					log.warn("scheduler_forced_shutdown component=OpenDotaRateLimitingService");
					scheduler.shutdownNow();
				}
			}
			catch (InterruptedException e) {
				log.warn("scheduler_shutdown_interrupted component=OpenDotaRateLimitingService", e);
				scheduler.shutdownNow();
				Thread.currentThread().interrupt();
			}
		}

		// Final save to database
		forceDatabaseSync();

		log.info(
				"service_shutdown_complete component=OpenDotaRateLimitingService totalRequests={} rejectedRequests={} successfulSyncs={}",
				totalRequests.get(), rejectedRequests.get(), successfulSyncs.get());
	}

	/**
	 * Attempts to acquire a permit for making an API request.
	 */
	public RateLimitResult tryAcquirePermit(String endpoint) {
		long startTime = System.nanoTime();
		totalRequests.incrementAndGet();

		try {
			// Fast path check
			if (isDailyLimitExceeded()) {
				rejectedRequests.incrementAndGet();
				int remaining = getRemainingDailyRequests();
				long resetTime = getTimeUntilDailyReset();

				log.warn("daily_limit_exceeded endpoint={} remaining={} resetInMs={}", endpoint, remaining, resetTime);

				return RateLimitResult.rejected("Daily limit exceeded", remaining, resetTime);
			}

			TokenBucket bucket = globalTokenBucket;
			if (bucket == null) {
				rejectedRequests.incrementAndGet();
				log.error("service_not_initialized component=OpenDotaRateLimitingService");
				return RateLimitResult.rejected("Service not initialized", 0, 0);
			}

			// Immediate acquisition attempt
			if (bucket.tryAcquire()) {
				recordSuccessfulRequest();
				long durationMs = (System.nanoTime() - startTime) / 1_000_000;
				// Hot path success - TRACE level to avoid log noise
				log.trace("rate_limit_acquired endpoint={} durationMs={} availableTokens={}", endpoint, durationMs,
						bucket.getAvailableTokens());
				return RateLimitResult.success();
			}

			// No tokens - compute wait and try waiting
			try {
				long waitTime = bucket.getTimeUntilNextToken();
				log.debug("rate_limit_waiting endpoint={} waitMs={} availableTokens={}", endpoint, waitTime,
						bucket.getAvailableTokens());

				boolean acquired = bucket.tryAcquireWithWait(waitTime);

				if (acquired) {
					recordSuccessfulRequest();
					long totalDurationMs = (System.nanoTime() - startTime) / 1_000_000;
					log.trace(
							"rate_limit_acquired_after_wait endpoint={} waitedMs={} totalDurationMs={} availableTokens={}",
							endpoint, waitTime, totalDurationMs, bucket.getAvailableTokens());
					return RateLimitResult.success(waitTime);
				}
				else {
					rejectedRequests.incrementAndGet();
					long nextTokenIn = bucket.getTimeUntilNextToken();
					log.warn("rate_limit_timeout endpoint={} availableTokens={} nextTokenInMs={}", endpoint,
							bucket.getAvailableTokens(), nextTokenIn);
					return RateLimitResult.rejected("Timeout waiting for rate limit", bucket.getAvailableTokens(),
							bucket.getTimeUntilNextToken());
				}
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				rejectedRequests.incrementAndGet();
				log.warn("rate_limit_interrupted endpoint={}", endpoint, e);
				return RateLimitResult.rejected("Interrupted while waiting", bucket.getAvailableTokens(), 0);
			}

		}
		catch (Exception e) {
			rejectedRequests.incrementAndGet();
			log.error("rate_limit_internal_error endpoint={}", endpoint, e);
			return RateLimitResult.rejected("Internal error", 0, 0);
		}
	}

	/**
	 * Records a successful API request for metrics and persistence
	 */
	private void recordSuccessfulRequest() {
		try {
			stateLock.readLock().lock();
			try {
				ApiRateLimit current = this.globalRateLimit;
				if (current != null) {
					updateDailyRequests();
					pendingChanges.incrementAndGet();

					// Trigger immediate sync if we have many pending changes
					if (pendingChanges.get() >= 10) {
						triggerImmediateSync();
					}
				}
			}
			finally {
				stateLock.readLock().unlock();
			}
		}
		catch (Exception e) {
			log.error("record_success_error component=OpenDotaRateLimitingService", e);
		}
	}

	private void updateDailyRequests() {
		ApiRateLimit current = this.globalRateLimit;
		if (current == null) {
			return;
		}

		LocalDate today = LocalDate.ofInstant(Instant.now(), ZoneOffset.UTC);
		if (!current.getDailyWindowStart().equals(today)) {
			// New day - reset counter
			log.info("daily_window_reset previousRequests={} newWindowStart={}", current.getDailyRequests(), today);
			current.setDailyWindowStart(today);
			current.setDailyRequests(1);
		}
		else {
			current.setDailyRequests(current.getDailyRequests() + 1);
		}
	}

	/**
	 * Gets current rate limiting status and metrics
	 */
	public RateLimitStatus getStatus() {
		stateLock.readLock().lock();
		try {
			TokenBucket bucket = globalTokenBucket;
			return RateLimitStatus.builder()
				.availableTokens(bucket != null ? bucket.getAvailableTokens() : 0)
				.remainingDailyRequests(getRemainingDailyRequests())
				.totalRequests(totalRequests.get())
				.rejectedRequests(rejectedRequests.get())
				.successRate(calculateSuccessRate())
				.timeUntilDailyReset(getTimeUntilDailyReset())
				.pendingChanges(pendingChanges.get())
				.lastSyncAge(System.currentTimeMillis() - lastSuccessfulSync.get())
				.build();
		}
		finally {
			stateLock.readLock().unlock();
		}
	}

	private void initializeGlobalRateLimit() {
		try {
			log.debug("db_load_attempt component=OpenDotaRateLimitingService endpoint={}", GLOBAL_TRACKING_ENDPOINT);

			this.globalRateLimit = rateLimitRepository.findByEndpoint(GLOBAL_TRACKING_ENDPOINT).orElseGet(() -> {
				log.info("db_entry_missing creating_new_global_rate_limit endpoint={}", GLOBAL_TRACKING_ENDPOINT);
				return createNewGlobalRateLimit();
			});

			log.info("db_loaded component=OpenDotaRateLimitingService dailyRequests={} windowStart={}",
					globalRateLimit.getDailyRequests(), globalRateLimit.getDailyWindowStart());

		}
		catch (DataAccessException e) {
			databaseSyncErrors.incrementAndGet();
			log.warn("db_error_fallback component=OpenDotaRateLimitingService using_in_memory fallback=true", e);
			this.globalRateLimit = createNewGlobalRateLimit();
		}
		catch (Exception e) {
			databaseSyncErrors.incrementAndGet();
			log.error("db_unexpected_error component=OpenDotaRateLimitingService", e);
			this.globalRateLimit = createNewGlobalRateLimit();
		}
	}

	/**
	 * Improved database synchronization strategy
	 */
	private void periodicDatabaseSync() {
		if (shouldSync()) {
			syncToDatabase();
		}
	}

	private boolean shouldSync() {
		long now = System.currentTimeMillis();
		long timeSinceLastSync = now - lastDatabaseSync.get();

		// Sync if we have pending changes and enough time has passed
		return pendingChanges.get() > 0 && timeSinceLastSync >= MIN_SYNC_INTERVAL_MS;
	}

	private void triggerImmediateSync() {
		if (scheduler != null && !scheduler.isShutdown()) {
			scheduler.submit(this::syncToDatabase);
			log.debug("triggered_immediate_sync pendingChanges={}", pendingChanges.get());
		}
		else {
			log.debug("triggered_immediate_sync_skipped scheduler_not_running");
		}
	}

	private void forceDatabaseSync() {
		syncToDatabase();
	}

	@Transactional
	private void syncToDatabase() {
		if (globalRateLimit == null) {
			log.debug("sync_noop_no_global_state");
			return;
		}

		stateLock.writeLock().lock();
		try {
			long syncStart = System.currentTimeMillis();

			// Find the managed entity and update it
			ApiRateLimit managed = rateLimitRepository.findByEndpoint(GLOBAL_TRACKING_ENDPOINT).orElse(globalRateLimit);

			int previousRequests = managed.getDailyRequests();
			managed.setDailyRequests(globalRateLimit.getDailyRequests());
			managed.setDailyWindowStart(globalRateLimit.getDailyWindowStart());
			managed.setWindowStart(Instant.now());

			rateLimitRepository.save(managed);

			// Update tracking variables
			lastDatabaseSync.set(syncStart);
			lastSuccessfulSync.set(syncStart);
			int changesSynced = pendingChanges.getAndSet(0);
			successfulSyncs.incrementAndGet();

			long syncDuration = System.currentTimeMillis() - syncStart;

			log.debug(
					"sync_success component=OpenDotaRateLimitingService changesSynced={} previousDailyRequests={} newDailyRequests={} durationMs={}",
					changesSynced, previousRequests, managed.getDailyRequests(), syncDuration);

		}
		catch (DataAccessException e) {
			databaseSyncErrors.incrementAndGet();
			log.warn("sync_db_failure will_retry component=OpenDotaRateLimitingService", e);
		}
		catch (Exception e) {
			databaseSyncErrors.incrementAndGet();
			log.error("sync_unexpected_error component=OpenDotaRateLimitingService", e);
		}
		finally {
			stateLock.writeLock().unlock();
		}
	}

	/**
	 * Log current metrics - called periodically
	 */
	private void logMetrics() {
		try {
			RateLimitStatus status = getStatus();
			log.info(
					"rate_limit_metrics availableTokens={} remainingDaily={} totalRequests={} rejectedRequests={} successRate={} pendingChanges={} lastSyncAgeMs={} dbErrors={}",
					status.availableTokens(), status.remainingDailyRequests(), status.totalRequests(),
					status.rejectedRequests(), String.format("%.2f", status.successRate()), status.pendingChanges(),
					status.lastSyncAge(), databaseSyncErrors.get());
		}
		catch (Exception e) {
			log.warn("metrics_log_error component=OpenDotaRateLimitingService", e);
		}
	}

	private boolean isDailyLimitExceeded() {
		return getRemainingDailyRequests() <= 0;
	}

	private int getRemainingDailyRequests() {
		try {
			stateLock.readLock().lock();
			try {
				ApiRateLimit current = this.globalRateLimit;
				if (current == null) {
					return requestsPerDay;
				}

				int remaining = requestsPerDay - current.getDailyRequests();

				if (remaining <= 0) {
					throw new RateLimitExceededException("N/A", remaining, 0);
				}

				if (remaining <= 10) {
					log.warn("daily_limit_nearing_exhaustion remaining={}", remaining);
				}

				return remaining;
			}
			finally {
				stateLock.readLock().unlock();
			}
		}
		catch (Exception e) {
			log.error("remaining_calculation_error failing_open", e);
			return requestsPerDay; // Fail open
		}
	}

	private long getTimeUntilDailyReset() {
		Instant now = Instant.now();
		Instant nextDay = now.plus(Duration.ofDays(1));
		return Duration.between(now, nextDay).toMillis();
	}

	private double calculateSuccessRate() {
		long total = totalRequests.get();
		if (total == 0) {
			return 100.0;
		}

		long successful = total - rejectedRequests.get();
		return (double) successful / total * 100.0;
	}

	private ApiRateLimit createNewGlobalRateLimit() {
		Instant now = Instant.now();
		LocalDate today = LocalDate.ofInstant(now, ZoneOffset.UTC);
		return ApiRateLimit.builder()
			.endpoint(GLOBAL_TRACKING_ENDPOINT)
			.requestsCount(0)
			.windowStart(now)
			.dailyRequests(0)
			.dailyWindowStart(today)
			.build();
	}

	/**
	 * Thread-safe token bucket implementation for smooth rate limiting.
	 */
	private static class TokenBucket {

		private static final Logger bucketLog = LoggerFactory.getLogger(TokenBucket.class);

		private final AtomicInteger currentTokens;

		private final AtomicLong lastTokenAcquisitionTime;

		private final int maxTokens;

		private final long refillIntervalMs;

		public TokenBucket(int maxTokens, long refillIntervalMs) {
			this.maxTokens = maxTokens;
			this.refillIntervalMs = refillIntervalMs;
			long now = System.currentTimeMillis();
			this.lastTokenAcquisitionTime = new AtomicLong(now);
			this.currentTokens = new AtomicInteger(maxTokens);

			bucketLog.debug("token_bucket_created maxTokens={} refillIntervalMs={}", maxTokens, refillIntervalMs);
		}

		public boolean tryAcquire() {
			return tryAcquireToken();
		}

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
					lastTokenAcquisitionTime.set(System.currentTimeMillis());
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
			long timeSinceLastAcquisition = System.currentTimeMillis() - lastTokenAcquisitionTime.get();
			return Math.max(0, refillIntervalMs - timeSinceLastAcquisition);
		}

		private void refillTokens() {
			long now = System.currentTimeMillis();
			long lastRefill = lastTokenAcquisitionTime.get();
			long timeSinceLastRefill = now - lastRefill;

			if (timeSinceLastRefill >= refillIntervalMs) {
				if (lastTokenAcquisitionTime.compareAndSet(lastRefill, now)) {
					int oldTokens = currentTokens.getAndSet(maxTokens);
					if (oldTokens == 0) {
						bucketLog.debug("token_bucket_refilled tokensAvailable={}", maxTokens);
					}
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
			long rejectedRequests, double successRate, long timeUntilDailyReset, int pendingChanges, long lastSyncAge) {

	}

}
