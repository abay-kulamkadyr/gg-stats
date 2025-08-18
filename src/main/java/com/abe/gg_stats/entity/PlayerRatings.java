package com.abe.gg_stats.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "player_ratings")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlayerRatings {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne
	@JoinColumn(name = "account_id")
	private Player player;

	@Column(name = "solo_competitive_rank")
	private Integer soloCompetitiveRank;

	@Column(name = "competitive_rank")
	private Integer competitiveRank;

	@Column(name = "updated_at", insertable = false, updatable = false)
	private LocalDateTime updatedAt;

}