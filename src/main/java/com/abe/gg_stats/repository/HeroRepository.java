package com.abe.gg_stats.repository;

import com.abe.gg_stats.entity.Hero;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface HeroRepository extends JpaRepository<Hero, Integer> {

	Optional<Hero> findByName(String name);

	@Query("SELECT id FROM Hero")
	List<Integer> findAllIds();

	@Query("SELECT updatedAt FROM Hero")
	List<LocalDateTime> findAllUpdates();

}