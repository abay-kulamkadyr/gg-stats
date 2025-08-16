package com.abe.gg_stats.repository;

import com.abe.gg_stats.entity.ApiRateLimit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface ApiRateLimitRepository extends JpaRepository<ApiRateLimit, Long> {

	Optional<ApiRateLimit> findByEndpoint(String endpoint);

	@Query("SELECT SUM(arl.dailyRequests) FROM ApiRateLimit arl WHERE arl.dailyWindowStart = ?1")
	Integer getTotalDailyRequests(LocalDate date);

	@Modifying
	@Query("UPDATE ApiRateLimit arl SET arl.requestsCount = arl.requestsCount + 1, arl.dailyRequests = arl.dailyRequests + 1, arl.updatedAt = CURRENT_TIMESTAMP WHERE arl.id = ?1")
	void incrementRequestCounts(Long id);

	@Modifying
	@Query("UPDATE ApiRateLimit arl SET arl.requestsCount = 1, arl.windowStart = ?2, arl.updatedAt = CURRENT_TIMESTAMP WHERE arl.id = ?1")
	void resetMinuteWindow(Long id, LocalDateTime newWindowStart);

	@Modifying
	@Query("UPDATE ApiRateLimit arl SET arl.dailyRequests = 1, arl.dailyWindowStart = ?2, arl.updatedAt = CURRENT_TIMESTAMP WHERE arl.id = ?1")
	void resetDailyWindow(Long id, LocalDate newDailyWindowStart);

	@Query("SELECT arl FROM ApiRateLimit arl WHERE arl.endpoint = ?1 AND arl.windowStart >= ?2")
	Optional<ApiRateLimit> findByEndpointAndWindowStartAfter(String endpoint, LocalDateTime windowStart);

	@Query("SELECT COUNT(arl) FROM ApiRateLimit arl WHERE arl.dailyWindowStart = ?1")
	Long countActiveEndpointsToday(LocalDate date);

}