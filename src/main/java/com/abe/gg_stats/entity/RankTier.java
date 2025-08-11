package com.abe.gg_stats.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "rank_tier")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RankTier {

	@Id
	@Column(name = "account_id")
	private Long accountId;

	@OneToOne
	@MapsId
	@JoinColumn(name = "account_id")
	private Player player;

	private Integer rating;

	@Column(name = "rank_tier")
	private Integer rankTier;

	@CreationTimestamp
	@Column(name = "created_at")
	private LocalDateTime createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at")
	private LocalDateTime updatedAt;

}