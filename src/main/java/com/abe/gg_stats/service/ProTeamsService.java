package com.abe.gg_stats.service;

import com.abe.gg_stats.dto.response.TeamDto;
import com.abe.gg_stats.dto.request.mapper.TeamMapper;
import com.abe.gg_stats.entity.Team;
import com.abe.gg_stats.repository.TeamRepository;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
public class ProTeamsService {

	private final TeamRepository teamRepository;

	private final TeamMapper teamMapper;

	@Autowired
	public ProTeamsService(TeamRepository teamRepository, TeamMapper teamMapper) {
		this.teamRepository = teamRepository;
		this.teamMapper = teamMapper;
	}

	public List<TeamDto> getAllTeams() {
		return teamRepository.findAllOrderByRatingDesc()
			.stream()
			.map(teamMapper::toTeamDto)
			.collect(Collectors.toList());
	}

	public List<TeamDto> getPaginatedTeams(int page, int size) {
		if (page < 0) {
			page = 0;
		}
		if (size <= 0 || size > 200) {
			size = 24;
		}

		PageRequest pr = PageRequest.of(page, size);
		Page<Team> p = teamRepository.findAllOrderByRatingDescNullsLast(pr);

		return p.getContent().stream().map(teamMapper::toTeamDto).collect(Collectors.toList());
	}

}