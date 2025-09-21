package com.abe.gg_stats.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.abe.gg_stats.dto.request.mapper.TeamMapper;
import com.abe.gg_stats.dto.response.TeamDto;
import com.abe.gg_stats.entity.Team;
import com.abe.gg_stats.repository.TeamRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

class ProTeamsServiceTest {

	private TeamRepository teamRepository;

	private TeamMapper teamMapper;

	private ProTeamsService service;

	@BeforeEach
	void setUp() {
		teamRepository = mock(TeamRepository.class);
		teamMapper = mock(TeamMapper.class);
		service = new ProTeamsService(teamRepository, teamMapper);
	}

	@Test
	void getAllTeamsMapsEntities() {
		Team t = new Team();
		when(teamRepository.findAllOrderByRatingDesc()).thenReturn(List.of(t));
		TeamDto dto = new TeamDto(1L, null, null, "https://exampleurl.com", 0);
		when(teamMapper.toTeamDto(t)).thenReturn(dto);

		List<TeamDto> res = service.getAllTeams();
		assertEquals(1, res.size());
		assertEquals(dto, res.getFirst());
	}

	@Test
	void getPaginatedTeamsNormalizesPageAndSize() {
		Team t = new Team();
		Page<Team> page = new PageImpl<>(List.of(t));
		when(teamRepository.findAllOrderByRatingDescNullsLast(any(PageRequest.class))).thenReturn(page);
		TeamDto dto = new TeamDto(1L, null, null, "https://example-url.com", 0);
		when(teamMapper.toTeamDto(t)).thenReturn(dto);

		// invalid inputs should normalize to page=0 and size=24
		List<TeamDto> res = service.getPaginatedTeams(-1, -5);
		assertEquals(1, res.size());
		verify(teamRepository).findAllOrderByRatingDescNullsLast(PageRequest.of(0, 24));
	}

}
