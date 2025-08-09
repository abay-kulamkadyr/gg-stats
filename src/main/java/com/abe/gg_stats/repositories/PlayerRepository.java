package com.abe.gg_stats.repositories;

import com.abe.gg_stats.entities.Player;
import org.springframework.data.repository.CrudRepository;

public interface PlayerRepository extends CrudRepository<Player, Long> {

}
