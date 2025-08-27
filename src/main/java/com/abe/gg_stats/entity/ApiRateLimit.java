package com.abe.gg_stats.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.ZonedDateTime;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "api_rate_limit")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiRateLimit {

	@Id
	private Long id;

	@Column(nullable = false, unique = true)
	private String endpoint;

	@Column(name = "requests_count", nullable = false)
	@Builder.Default
	private Integer requestsCount = 0;

	@Column(name = "window_start", nullable = false)
	private ZonedDateTime windowStart;

	@Column(name = "daily_requests", nullable = false)
	@Builder.Default
	private Integer dailyRequests = 0;

	@Column(name = "daily_window_start", nullable = false)
	private LocalDate dailyWindowStart;

	@Column(name = "updated_at", insertable = false, updatable = false)
	private LocalDateTime updatedAt;

}