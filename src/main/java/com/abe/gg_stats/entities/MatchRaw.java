package com.abe.gg_stats.entities;

import java.time.Instant;
import lombok.Value;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Value
@Table("matches_raw")
public class MatchRaw {

	@Id
	Long matchId;

	String raw; // JSON stored here

	Instant fetchedAt;

}
