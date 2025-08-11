package com.abe.gg_stats.repository;

import com.abe.gg_stats.entity.Player;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PlayerRepository extends JpaRepository<Player, Long> {

	List<Player> findByLastMatchTimeAfter(LocalDateTime after);

	@Query("SELECT p FROM Player p WHERE p.plus = true")
	List<Player> findDotaPlusSubscribers();

}