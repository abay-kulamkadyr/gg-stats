package com.abe.gg_stats.repository;

import com.abe.gg_stats.entity.Player;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PlayerRepository extends JpaRepository<Player, Long> {

	@Query("SELECT p.accountId FROM Player p")
	Set<Long> findAllIds();

	Optional<List<Player>> findByLastMatchTimeAfter(LocalDateTime after);

	@Query("SELECT p FROM Player p WHERE p.plus = true")
	Optional<List<Player>> findDotaPlusSubscribers();

	@Query("SELECT MAX(p.updatedAt) FROM Player p")
	Optional<Instant> findMaxUpdatedAt();

	Optional<Player> findByAccountId(Long accountId);

}