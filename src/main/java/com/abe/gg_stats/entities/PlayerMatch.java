package com.abe.gg_stats.entities;

import java.time.Instant;
import lombok.Value;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Value
@Table("player_matches")
public class PlayerMatch {

	@Id
	Long id;

	Long matchId;

	Long accountId;

	Integer heroId;

	Boolean isRadiant;

	Boolean win;

	Integer kills;

	Integer deaths;

	Integer assists;

	Integer gpm;

	Integer xpm;

	Integer lastHits;

	Integer denies;

	Integer[] items;

	Integer lane;

	String role;

	Integer duration;

	Instant createdAt;

}
