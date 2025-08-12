package com.abe.gg_stats.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "leaderboard_rank")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LeaderboardRank {

	@Id
	@Column(name = "account_id")
	private Long accountId;

	@Column(name = "rank_position")
	private Integer rankPosition;

	private Integer rating;

	@CreationTimestamp
	@Column(name = "created_at")
	private LocalDateTime createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at")
	private LocalDateTime updatedAt;

}