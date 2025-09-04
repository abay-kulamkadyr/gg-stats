package com.abe.gg_stats.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "api_rate_limit")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiRateLimit {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true)
	private String endpoint;

	@Column(name = "requests_count", nullable = false)
	@Builder.Default
	private Integer requestsCount = 0;

	@Column(name = "window_start", nullable = false)
	private Instant windowStart;

	@Column(name = "daily_requests", nullable = false)
	@Builder.Default
	private Integer dailyRequests = 0;

	@Column(name = "daily_window_start", nullable = false)
	private LocalDate dailyWindowStart;

	@Column(name = "updated_at", insertable = false)
	private Instant updatedAt;

}