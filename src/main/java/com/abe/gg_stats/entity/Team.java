package com.abe.gg_stats.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "team")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Team {

	@Id
	@Column(name = "team_id")
	private Long teamId;

	private Integer rating;

	@Column(columnDefinition = "INT DEFAULT 0")
	private Integer wins;

	@Column(columnDefinition = "INT DEFAULT 0")
	private Integer losses;

	@Column(name = "last_match_time")
	private Long lastMatchTime;

	private String name;

	private String tag;

	@Column(name = "logo_url", columnDefinition = "TEXT")
	private String logoUrl;

	@CreationTimestamp
	@Column(name = "created_at")
	private LocalDateTime createdAt;

	@Column(name = "updated_at", insertable = false, updatable = false)
	private LocalDateTime updatedAt;

}