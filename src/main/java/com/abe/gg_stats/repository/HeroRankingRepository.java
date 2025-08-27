package com.abe.gg_stats.repository;

import com.abe.gg_stats.entity.HeroRanking;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface HeroRankingRepository extends JpaRepository<HeroRanking, Long> {

	@Query("SELECT hr FROM HeroRanking hr WHERE hr.heroId=?1 AND hr.accountId=?2")
	Optional<HeroRanking> findByHeroIdAndAccountId(Long accountId, Integer heroId);

	List<HeroRanking> findByHeroId(Integer heroId);

	List<HeroRanking> findByAccountId(Long accountId);

	@Query("SELECT MAX(hr.updatedAt) FROM HeroRanking hr")
	Optional<LocalDateTime> findMaxUpdatedAt();

}
