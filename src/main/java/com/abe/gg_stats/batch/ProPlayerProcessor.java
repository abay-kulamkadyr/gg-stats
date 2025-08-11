package com.abe.gg_stats.batch;

import com.abe.gg_stats.entity.Player;
import com.abe.gg_stats.entity.ProPlayer;
import com.abe.gg_stats.entity.Team;
import com.abe.gg_stats.repository.PlayerRepository;
import com.abe.gg_stats.repository.ProPlayerRepository;
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
public class ProPlayerProcessor implements ItemProcessor<JsonNode, ProPlayer> {

	private final PlayerRepository playerRepository;

	private final ProPlayerRepository proPlayerRepository;

	private final TeamRepository teamRepository;

	@Override
	public ProPlayer process(JsonNode item) throws Exception {
		try {
			if (!item.has("account_id") || item.get("account_id").isNull()) {
				return null; // Skip pro players without account_id
			}

			Long accountId = item.get("account_id").asLong();

			// First try to find existing ProPlayer
			Optional<ProPlayer> existingProPlayer = proPlayerRepository.findById(accountId);

			if (existingProPlayer.isPresent()) {
				// Update existing ProPlayer
				ProPlayer proPlayer = existingProPlayer.get();
				proPlayer.setName(item.has("name") ? item.get("name").asText() : null);
				proPlayer.setCountryCode(item.has("country_code") ? item.get("country_code").asText() : null);
				proPlayer.setFantasyRole(item.has("fantasy_role") ? item.get("fantasy_role").asInt() : null);
				proPlayer.setIsLocked(item.has("is_locked") ? item.get("is_locked").asBoolean() : false);
				proPlayer.setIsPro(item.has("is_pro") ? item.get("is_pro").asBoolean() : true);

				// Update team association
				if (item.has("team_id") && !item.get("team_id").isNull()) {
					Long teamId = item.get("team_id").asLong();
					Optional<Team> teamOpt = teamRepository.findById(teamId);
					teamOpt.ifPresent(proPlayer::setTeam);
				}

				return proPlayer;
			}

			// Create new ProPlayer if not found
			ProPlayer proPlayer = new ProPlayer();
			proPlayer.setAccountId(accountId);

			// Find or create player entity
			Optional<Player> playerOpt = playerRepository.findById(accountId);
			if (playerOpt.isEmpty()) {
				// Create basic player record for pro player
				Player player = new Player();
				player.setAccountId(accountId);
				player.setPersonName(item.has("name") ? item.get("name").asText() : null);
				player = playerRepository.save(player);
				proPlayer.setPlayer(player);
			}
			else {
				proPlayer.setPlayer(playerOpt.get());
			}

			proPlayer.setName(item.has("name") ? item.get("name").asText() : null);
			proPlayer.setCountryCode(item.has("country_code") ? item.get("country_code").asText() : null);
			proPlayer.setFantasyRole(item.has("fantasy_role") ? item.get("fantasy_role").asInt() : null);
			proPlayer.setIsLocked(item.has("is_locked") ? item.get("is_locked").asBoolean() : false);
			proPlayer.setIsPro(item.has("is_pro") ? item.get("is_pro").asBoolean() : true);

			// Handle team association
			if (item.has("team_id") && !item.get("team_id").isNull()) {
				Long teamId = item.get("team_id").asLong();
				Optional<Team> teamOpt = teamRepository.findById(teamId);
				teamOpt.ifPresent(proPlayer::setTeam);
			}

			return proPlayer;
		}
		catch (Exception e) {
			log.error("Error processing pro player: {}", item.toString(), e);
			return null;
		}
	}

}