package com.abe.gg_stats.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "pro_player")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProPlayer {

	@Id
	@Column(name = "account_id")
	private Long accountId;

	@OneToOne
	@MapsId
	@JoinColumn(name = "account_id")
	private Player player;

	@Column(name = "country_code")
	private String countryCode;

	@Column(name = "fantasy_role")
	private Integer fantasyRole;

	@ManyToOne
	@JoinColumn(name = "team_id")
	private Team team;

	private String name;

	@Column(name = "is_locked")
	private Boolean isLocked;

	@Column(name = "is_pro")
	private Boolean isPro;

	@CreationTimestamp
	@Column(name = "created_at")
	private LocalDateTime createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at")
	private LocalDateTime updatedAt;

}