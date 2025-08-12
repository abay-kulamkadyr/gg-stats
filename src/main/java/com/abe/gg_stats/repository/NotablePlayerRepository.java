package com.abe.gg_stats.repository;

import com.abe.gg_stats.entity.NotablePlayer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotablePlayerRepository extends JpaRepository<NotablePlayer, Long> {

	List<NotablePlayer> findByTeamTeamId(Long teamId);

	List<NotablePlayer> findByCountryCode(String countryCode);

	@Query("SELECT pp FROM NotablePlayer pp WHERE pp.isPro = true")
	List<NotablePlayer> findActivePros();

}