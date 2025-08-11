package com.abe.gg_stats.repository;

import com.abe.gg_stats.entity.ApiRateLimit;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface ApiRateLimitRepository extends JpaRepository<ApiRateLimit, Long> {

	Optional<ApiRateLimit> findByEndpoint(String endpoint);

	@Query("SELECT SUM(arl.dailyRequests) FROM ApiRateLimit arl WHERE arl.dailyWindowStart = ?1")
	Integer getTotalDailyRequests(LocalDate date);

}