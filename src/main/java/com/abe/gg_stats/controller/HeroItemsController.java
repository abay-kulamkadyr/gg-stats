package com.abe.gg_stats.controller;

import com.abe.gg_stats.repository.jdbc.HeroItemsDao;
import com.abe.gg_stats.repository.jdbc.HeroTopPlayersDao;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/pro")
@RequiredArgsConstructor
public class HeroItemsController {

	private final HeroItemsDao heroItemsDao;

	private final HeroTopPlayersDao heroTopPlayersDao;

	@GetMapping("/heroes/{heroId}/popular-items")
	public Map<String, Object> popularItems(@PathVariable("heroId") int heroId,
			@RequestParam(value = "limit", required = false, defaultValue = "12") int limit,
			@RequestParam(value = "playersLimit", required = false, defaultValue = "10") int playersLimit) {
		Map<String, Object> resp = new HashMap<>();
		resp.put("start_game", heroItemsDao.topItemsForHero(heroId, "start_game", limit));
		resp.put("early_game", heroItemsDao.topItemsForHero(heroId, "early_game", limit));
		resp.put("mid_game", heroItemsDao.topItemsForHero(heroId, "mid_game", limit));
		resp.put("late_game", heroItemsDao.topItemsForHero(heroId, "late_game", limit));
		resp.put("top_players", heroTopPlayersDao.topPlayersForHero(heroId, playersLimit));
		return resp;
	}

}
