package com.abe.gg_stats.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

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

	@CreationTimestamp
	@Column(name = "recorded_at")
	private LocalDateTime recordedAt;

}