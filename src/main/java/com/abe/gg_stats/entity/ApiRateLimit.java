package com.abe.gg_stats.entity;

import jakarta.persistence.*;
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
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true)
	private String endpoint;

	@Column(name = "requests_count", nullable = false)
	@Builder.Default
	private Integer requestsCount = 0;

	@Column(name = "window_start", nullable = false)
	@Builder.Default
	private LocalDateTime windowStart = LocalDateTime.now();

	@Column(name = "daily_requests", nullable = false)
	@Builder.Default
	private Integer dailyRequests = 0;

	@Column(name = "daily_window_start", nullable = false)
	@Builder.Default
	private LocalDate dailyWindowStart = LocalDate.now();

	@Column(name = "updated_at")
	@Builder.Default
	private LocalDateTime updatedAt = LocalDateTime.now();

}