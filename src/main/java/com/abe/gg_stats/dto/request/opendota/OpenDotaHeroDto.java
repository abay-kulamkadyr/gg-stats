package com.abe.gg_stats.dto.request.opendota;

import java.util.List;

public record OpenDotaHeroDto(Integer id, String name, String localizedName, String primaryAttr, String attackType,
		List<String> roles) {

}
