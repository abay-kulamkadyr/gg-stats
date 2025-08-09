package com.abe.gg_stats.entities;

import java.time.Instant;
import lombok.Value;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Value
@Table("players")
public class Player {

	@Id
	Long accountId;

	String name;

	Instant lastSeen;

	Integer mmrEstimate;

	Integer rankTier;

	String profileUrl;

	String raw;

	Instant updatedAt;

}
