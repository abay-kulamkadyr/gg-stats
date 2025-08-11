package com.abe.gg_stats.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "player")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Player {

	@Id
	@Column(name = "account_id")
	private Long accountId;

	@Column(name = "steam_id")
	private String steamId;

	@Column(name = "avatar", columnDefinition = "TEXT")
	private String avatar;

	@Column(name = "avatarmedium", columnDefinition = "TEXT")
	private String avatarMedium;

	@Column(name = "avatarfull", columnDefinition = "TEXT")
	private String avatarFull;

	@Column(name = "profileurl", columnDefinition = "TEXT")
	private String profileUrl;

	@Column(name = "personname")
	private String personName;

	@Column(name = "last_login")
	private LocalDateTime lastLogin;

	@Column(name = "full_history_time")
	private LocalDateTime fullHistoryTime;

	@Column(name = "cheese")
	private Integer cheese;

	@Column(name = "fh_unavailable")
	private Boolean fhUnavailable;

	@Column(name = "loccountrycode")
	private String locCountryCode;

	@Column(name = "last_match_time")
	private LocalDateTime lastMatchTime;

	@Column(name = "plus")
	private Boolean plus;

	@CreationTimestamp
	@Column(name = "created_at")
	private LocalDateTime createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at")
	private LocalDateTime updatedAt;

}
