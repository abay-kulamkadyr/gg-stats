package com.abe.gg_stats.batch;

import com.abe.gg_stats.entity.Hero;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class HeroProcessor implements ItemProcessor<JsonNode, Hero> {

	@Override
	public Hero process(JsonNode item) throws Exception {
		try {
			Hero hero = new Hero();
			hero.setId(item.get("id").asInt());
			hero.setName(item.get("name").asText());
			hero.setLocalizedName(item.get("localized_name").asText());
			hero.setPrimaryAttr(item.has("primary_attr") ? item.get("primary_attr").asText() : null);
			hero.setAttackType(item.has("attack_type") ? item.get("attack_type").asText() : null);

			// Process roles array
			List<String> roles = new ArrayList<>();
			if (item.has("roles") && item.get("roles").isArray()) {
				item.get("roles").forEach(role -> roles.add(role.asText()));
			}
			hero.setRoles(roles);

			return hero;
		}
		catch (Exception e) {
			log.error("Error processing hero: {}", item.toString(), e);
			return null; // Skip this item
		}
	}

}