package com.abe.gg_stats.repository;

import com.abe.gg_stats.entity.ProPlayer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProPlayerRepository extends JpaRepository<ProPlayer, Long> {

	List<ProPlayer> findByTeamTeamId(Long teamId);

	List<ProPlayer> findByCountryCode(String countryCode);

	@Query("SELECT pp FROM ProPlayer pp WHERE pp.isPro = true")
	List<ProPlayer> findActivePros();

}