package com.abe.gg_stats.repository;

import com.abe.gg_stats.entity.ApiRateLimit;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface ApiRateLimitRepository extends JpaRepository<ApiRateLimit, Long> {

	Optional<ApiRateLimit> findByEndpoint(String endpoint);

	@Query("SELECT SUM(arl.dailyRequests) FROM ApiRateLimit arl WHERE arl.dailyWindowStart = ?1")
	Optional<Integer> getTotalDailyRequests(LocalDate date);

	@Query("SELECT SUM(arl.requestsCount) FROM ApiRateLimit arl WHERE arl.windowStart BETWEEN ?1 AND ?2")
	Optional<Long> getMinuteRequestWindowCount(ZonedDateTime startTime, ZonedDateTime endTime);

	@Query("SELECT MAX(arl.windowStart) FROM ApiRateLimit arl")
	Optional<ZonedDateTime> getLatestWindowStart();

}