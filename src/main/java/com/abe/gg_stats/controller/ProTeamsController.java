package com.abe.gg_stats.controller;

import com.abe.gg_stats.dto.response.TeamDto;
import com.abe.gg_stats.service.ProTeamsService;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/teams")
class ProTeamsController {

	private final ProTeamsService teamService;

	@Autowired
	ProTeamsController(ProTeamsService teamService) {
		this.teamService = teamService;
	}

	@GetMapping
	List<TeamDto> teams(@RequestParam(value = "page", required = false) Integer page,
			@RequestParam(value = "size", required = false, defaultValue = "24") int size) {

		if (page == null) {
			return teamService.getAllTeams();
		}
		return teamService.getPaginatedTeams(page, size);
	}

}