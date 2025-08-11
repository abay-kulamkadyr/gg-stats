package com.abe.gg_stats.batch;

import com.abe.gg_stats.entity.Team;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class TeamProcessor implements ItemProcessor<JsonNode, Team> {

	@Override
	public Team process(JsonNode item) throws Exception {
		try {
			Team team = new Team();
			team.setTeamId(item.get("team_id").asLong());
			team.setRating(item.has("rating") ? item.get("rating").asInt() : null);
			team.setWins(item.has("wins") ? item.get("wins").asInt() : 0);
			team.setLosses(item.has("losses") ? item.get("losses").asInt() : 0);
			team.setLastMatchTime(item.has("last_match_time") ? item.get("last_match_time").asLong() : null);
			team.setName(item.has("name") ? item.get("name").asText() : null);
			team.setTag(item.has("tag") ? item.get("tag").asText() : null);
			team.setLogoUrl(item.has("logo_url") ? item.get("logo_url").asText() : null);

			return team;
		}
		catch (Exception e) {
			log.error("Error processing team: {}", item.toString(), e);
			return null;
		}
	}

}