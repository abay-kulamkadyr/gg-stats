package com.abe.gg_stats.dto;

import java.util.List;

public record HeroDto(Integer id, String name, String localizedName, String primaryAttr, String attackType,
		List<String> roles) {

}
