package com.abe.gg_stats.repository;

import com.abe.gg_stats.entity.PlayerRatings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PlayerRatingsRepository extends JpaRepository<PlayerRatings, Long> {

	List<PlayerRatings> findByPlayerAccountIdOrderByRecordedAtDesc(Long accountId);

	@Query("SELECT pr FROM PlayerRatings pr WHERE pr.recordedAt >= ?1")
	List<PlayerRatings> findRecentRatings(LocalDateTime since);

}