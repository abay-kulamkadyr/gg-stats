package com.abe.gg_stats.dto.request.mapper;

import com.abe.gg_stats.dto.response.HeroDto;
import com.abe.gg_stats.entity.Hero;
import org.springframework.stereotype.Component;

@Component
public class HeroMapper {

	public HeroDto toHeroDto(Hero h) {
		String cdnName = h.getName() == null ? null : h.getName().replace("npc_dota_hero_", "").toLowerCase();
		String imgUrl = cdnName == null ? null
				: "https://cdn.steamstatic.com/apps/dota2/images/dota_react/heroes/" + cdnName + ".png";
		return new HeroDto(h.getId(), h.getName(), h.getLocalizedName(), cdnName, imgUrl);
	}

}
