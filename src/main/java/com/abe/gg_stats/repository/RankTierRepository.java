package com.abe.gg_stats.repository;

import com.abe.gg_stats.entity.RankTier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RankTierRepository extends JpaRepository<RankTier, Long> {

	@Query("SELECT rt FROM RankTier rt WHERE rt.rankTier >= ?1 ORDER BY rt.rating DESC")
	List<RankTier> findByMinimumRankTierOrderByRating(Integer minimumTier);

}