package com.abe.gg_stats.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "hero_ranking")
@IdClass(HeroRankingId.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HeroRanking {

	@Id
	@Column(name = "account_id")
	private Long accountId;

	@Id
	@Column(name = "hero_id")
	private Integer heroId;

	@Column(name = "score")
	private Double score;

	@CreationTimestamp
	@Column(name = "created_at")
	private LocalDateTime createdAt;

	@Column(name = "updated_at", insertable = false, updatable = false)
	private LocalDateTime updatedAt;

}
