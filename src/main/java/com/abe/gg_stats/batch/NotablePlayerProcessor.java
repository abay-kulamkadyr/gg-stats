package com.abe.gg_stats.batch;

import com.abe.gg_stats.entity.NotablePlayer;
import com.abe.gg_stats.entity.Team;
import com.abe.gg_stats.repository.NotablePlayerRepository;
import com.abe.gg_stats.repository.TeamRepository;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotablePlayerProcessor implements ItemProcessor<JsonNode, NotablePlayer> {

	private final NotablePlayerRepository notablePlayerRepository;

	private final TeamRepository teamRepository;

	@Override
	public NotablePlayer process(JsonNode item) throws Exception {
		try {
			if (!item.has("account_id") || item.get("account_id").isNull()) {
				return null; // Skip pro players without account_id
			}

			Long accountId = item.get("account_id").asLong();

			// First try to find existing ProPlayer
			Optional<NotablePlayer> existingNotablePlayer = notablePlayerRepository.findById(accountId);

			if (existingNotablePlayer.isPresent()) {
				// Update existing ProPlayer
				NotablePlayer notablePlayer = existingNotablePlayer.get();
				notablePlayer.setName(item.has("name") ? item.get("name").asText() : null);
				notablePlayer.setCountryCode(item.has("country_code") ? item.get("country_code").asText() : null);
				notablePlayer.setFantasyRole(item.has("fantasy_role") ? item.get("fantasy_role").asInt() : null);
				notablePlayer.setIsLocked(item.has("is_locked") ? item.get("is_locked").asBoolean() : false);
				notablePlayer.setIsPro(item.has("is_pro") ? item.get("is_pro").asBoolean() : true);

				// Update team association
				if (item.has("team_id") && !item.get("team_id").isNull()) {
					Long teamId = item.get("team_id").asLong();
					Optional<Team> teamOpt = teamRepository.findById(teamId);
					teamOpt.ifPresent(notablePlayer::setTeam);
				}

				return notablePlayer;
			}

			// Create new ProPlayer if not found
			NotablePlayer notablePlayer = new NotablePlayer();
			notablePlayer.setAccountId(accountId);
			notablePlayer.setName(item.has("name") ? item.get("name").asText() : null);
			notablePlayer.setCountryCode(item.has("country_code") ? item.get("country_code").asText() : null);
			notablePlayer.setFantasyRole(item.has("fantasy_role") ? item.get("fantasy_role").asInt() : null);
			notablePlayer.setIsLocked(item.has("is_locked") ? item.get("is_locked").asBoolean() : false);
			notablePlayer.setIsPro(item.has("is_pro") ? item.get("is_pro").asBoolean() : true);

			// Handle team association
			if (item.has("team_id") && !item.get("team_id").isNull()) {
				Long teamId = item.get("team_id").asLong();
				Optional<Team> teamOpt = teamRepository.findById(teamId);
				teamOpt.ifPresent(notablePlayer::setTeam);
			}

			return notablePlayer;
		}
		catch (Exception e) {
			log.error("Error processing pro player: {}", item.toString(), e);
			return null;
		}
	}

}