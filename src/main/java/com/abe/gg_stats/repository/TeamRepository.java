package com.abe.gg_stats.repository;

import com.abe.gg_stats.entity.Team;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface TeamRepository extends JpaRepository<Team, Long> {

	Optional<Team> findByName(String name);

	@Query("SELECT t FROM Team t ORDER BY t.rating DESC")
	List<Team> findAllOrderByRatingDesc();

	@Query(value = "SELECT t FROM Team t ORDER BY CASE WHEN t.rating IS NULL THEN 1 ELSE 0 END, t.rating DESC",
			countQuery = "SELECT COUNT(t) FROM Team t")
	Page<Team> findAllOrderByRatingDescNullsLast(Pageable pageable);

	@Query("SELECT MAX(t.updatedAt) FROM Team t")
	Optional<Instant> findMaxUpdatedAt();

}
