package com.abe.gg_stats.dto.request.opendota;

public record OpenDotaNotablePlayerDto(Long accountId, String countryCode, Integer fantasyRole, String name,
		Boolean isLocked, Boolean isPro, Long teamId) {

}
