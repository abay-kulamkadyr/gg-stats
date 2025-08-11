package com.abe.gg_stats.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "api_rate_limit")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiRateLimit {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String endpoint;

	@Column(name = "requests_count")
	private Integer requestsCount;

	@Column(name = "window_start")
	private LocalDateTime windowStart;

	@Column(name = "daily_requests")
	private Integer dailyRequests;

	@Column(name = "daily_window_start")
	private LocalDate dailyWindowStart;

}