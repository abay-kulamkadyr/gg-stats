package com.abe.gg_stats.dto;

public record NotablePlayerDto(Long accountId, String countryCode, Integer fantasyRole, String name, Boolean isLocked,
		Boolean isPro, Long teamId) {
}
