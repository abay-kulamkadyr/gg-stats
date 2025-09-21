package com.abe.gg_stats.controller;

import com.abe.gg_stats.dto.response.HeroDto;
import com.abe.gg_stats.dto.request.mapper.HeroMapper;
import com.abe.gg_stats.repository.HeroRepository;
import com.abe.gg_stats.repository.jdbc.HeroItemsDao;
import com.abe.gg_stats.repository.jdbc.HeroTopPlayersDao;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@ResponseBody
@RequestMapping("/heroes")
class HeroesController {

	private final HeroRepository heroRepository;

	private final HeroMapper heroMapper;

	private final HeroItemsDao heroItemsDao;

	private final HeroTopPlayersDao heroTopPlayersDao;

	@Autowired
	HeroesController(HeroRepository heroRepository, HeroMapper heroMapper, HeroItemsDao heroItemsDao,
			HeroTopPlayersDao heroTopPlayersDao) {
		this.heroRepository = heroRepository;
		this.heroMapper = heroMapper;
		this.heroItemsDao = heroItemsDao;
		this.heroTopPlayersDao = heroTopPlayersDao;
	}

	@GetMapping
	List<HeroDto> heroes() {
		return heroRepository.findAll().stream().map(heroMapper::toHeroDto).collect(Collectors.toList());
	}

	@GetMapping("/{heroId}/popular-items")
	Map<String, Object> popularItems(@PathVariable("heroId") int heroId,
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