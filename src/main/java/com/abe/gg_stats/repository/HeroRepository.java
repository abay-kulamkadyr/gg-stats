package com.abe.gg_stats.repository;

import com.abe.gg_stats.entity.Hero;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface HeroRepository extends JpaRepository<Hero, Integer> {

	Optional<Hero> findByName(String name);

}