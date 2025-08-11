package com.abe.gg_stats.repository;

import com.abe.gg_stats.entity.LeaderboardRank;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LeaderboardRankRepository extends JpaRepository<LeaderboardRank, Long> {

	@Query("SELECT lr FROM LeaderboardRank lr ORDER BY lr.rankPosition ASC")
	List<LeaderboardRank> findAllOrderByPosition();

	List<LeaderboardRank> findTop1000ByOrderByRankPositionAsc();

}
