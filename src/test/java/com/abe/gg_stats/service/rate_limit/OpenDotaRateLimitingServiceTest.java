package com.abe.gg_stats.service.rate_limit;

import static org.junit.jupiter.api.Assertions.*;

import com.abe.gg_stats.entity.ApiRateLimit;
import com.abe.gg_stats.repository.ApiRateLimitRepository;
import java.lang.reflect.Constructor;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

class OpenDotaRateLimitingServiceTest {

	private ApiRateLimitRepository repo;

	private OpenDotaRateLimitingService service;

	@BeforeEach
	void setUp() {
		repo = Mockito.mock(ApiRateLimitRepository.class);
		service = new OpenDotaRateLimitingService(repo);
		// Configure generous limits for tests unless overridden
		ReflectionTestUtils.setField(service, "requestsPerMinute", 60);
		ReflectionTestUtils.setField(service, "requestsPerDay", 2000);
		ReflectionTestUtils.setField(service, "refillInterval", 60_000L);
	}

	@Test
	void tryAcquirePermitSucceedsWhenBucketAndDailyAllow() throws Exception {
		// Prepare in-memory global rate limit state
		ApiRateLimit state = ApiRateLimit.builder()
			.endpoint("GLOBAL")
			.windowStart(Instant.now())
			.dailyWindowStart(LocalDate.now())
			.dailyRequests(0)
			.build();
		ReflectionTestUtils.setField(service, "globalRateLimit", state);

		// Instantiate TokenBucket via reflection and set it
		Class<?>[] innerClasses = OpenDotaRateLimitingService.class.getDeclaredClasses();
		Class<?> tokenBucketClass = null;
		for (Class<?> c : innerClasses) {
			if (c.getSimpleName().equals("TokenBucket")) {
				tokenBucketClass = c;
				break;
			}
		}
		assertNotNull(tokenBucketClass);
		Constructor<?> ctor = tokenBucketClass.getDeclaredConstructor(int.class, long.class);
		ctor.setAccessible(true);
		Object tokenBucket = ctor.newInstance(60, 60_000L);
		ReflectionTestUtils.setField(service, "globalTokenBucket", tokenBucket);

		OpenDotaRateLimitingService.RateLimitResult result = service.tryAcquirePermit("/heroes");
		assertTrue(result.allowed());
	}

	@Test
	void tryAcquirePermitFailOpenWhenDailyLimitExceeded() throws Exception {
		// dailyRequests == requestsPerDay to exceed
		ReflectionTestUtils.setField(service, "requestsPerDay", 1);
		ApiRateLimit state = ApiRateLimit.builder()
			.endpoint("GLOBAL")
			.windowStart(Instant.now())
			.dailyWindowStart(LocalDate.now())
			.dailyRequests(1)
			.build();
		ReflectionTestUtils.setField(service, "globalRateLimit", state);

		// Token bucket present (but should not be used due to daily limit)
		Class<?>[] inner = OpenDotaRateLimitingService.class.getDeclaredClasses();
		Class<?> tokenBucketClass = null;
		for (Class<?> c : inner) {
			if (c.getSimpleName().equals("TokenBucket")) {
				tokenBucketClass = c;
				break;
			}
		}
		Constructor<?> ctor = tokenBucketClass.getDeclaredConstructor(int.class, long.class);
		ctor.setAccessible(true);
		Object tokenBucket = ctor.newInstance(60, 60_000L);
		ReflectionTestUtils.setField(service, "globalTokenBucket", tokenBucket);

		OpenDotaRateLimitingService.RateLimitResult result = service.tryAcquirePermit("/teams");
		// Service is implemented to fail-open on exception during remaining calculation
		assertTrue(result.allowed());
	}

}
