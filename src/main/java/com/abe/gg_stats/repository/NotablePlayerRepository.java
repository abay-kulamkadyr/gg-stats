package com.abe.gg_stats.repository;

import com.abe.gg_stats.entity.NotablePlayer;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotablePlayerRepository extends JpaRepository<NotablePlayer, Long> {

	List<NotablePlayer> findByTeamTeamId(Long teamId);

	List<NotablePlayer> findByCountryCode(String countryCode);

	Optional<NotablePlayer> findByAccountId(Long accountId);

	@Query("SELECT pp FROM NotablePlayer pp WHERE pp.isPro = true")
	List<NotablePlayer> findActivePros();

	@Query("SELECT np.accountId FROM NotablePlayer np")
	Set<Long> findAllIds();

	@Query("SELECT MAX(np.updatedAt) FROM NotablePlayer np")
	Optional<Instant> findMaxUpdatedAt();

}