package com.abe.gg_stats.controller;

import com.abe.gg_stats.entity.Hero;
import com.abe.gg_stats.entity.Team;
import com.abe.gg_stats.repository.HeroRepository;
import com.abe.gg_stats.repository.TeamRepository;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/pro")
@RequiredArgsConstructor
public class CatalogController {

	private final HeroRepository heroRepository;

	private final TeamRepository teamRepository;

	@GetMapping("/heroes")
	public List<HeroDto> heroes() {
		return heroRepository.findAll().stream().map(CatalogController::toHeroDto).collect(Collectors.toList());
	}

	@GetMapping("/teams")
	public List<TeamDto> teams() {
		return teamRepository.findAllOrderByRatingDesc()
			.stream()
			.map(CatalogController::toTeamDto)
			.collect(Collectors.toList());
	}

	@GetMapping("/teams/paged")
	public PageResponse<TeamDto> teamsPaged(
			@RequestParam(value = "page", required = false, defaultValue = "0") int page,
			@RequestParam(value = "size", required = false, defaultValue = "24") int size) {
		if (page < 0)
			page = 0;
		if (size <= 0 || size > 200)
			size = 24;
		PageRequest pr = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "rating"));
		Page<Team> p = teamRepository.findAll(pr);
		List<TeamDto> content = p.getContent().stream().map(CatalogController::toTeamDto).collect(Collectors.toList());
		return new PageResponse<>(content, p.getNumber(), p.getSize(), p.getTotalPages(), p.getTotalElements());
	}

	private static HeroDto toHeroDto(Hero h) {
		String cdnName = h.getName() == null ? null : h.getName().replace("npc_dota_hero_", "").toLowerCase();
		String imgUrl = cdnName == null ? null
				: "https://cdn.steamstatic.com/apps/dota2/images/dota_react/heroes/" + cdnName + ".png";
		return new HeroDto(h.getId(), h.getName(), h.getLocalizedName(), cdnName, imgUrl);
	}

	private static TeamDto toTeamDto(Team t) {
		return new TeamDto(t.getTeamId(), t.getName(), t.getTag(), t.getLogoUrl(), t.getRating());
	}

	public record HeroDto(Integer heroId, String name, String localizedName, String heroCdnName, String heroImgUrl) {
	}

	public record TeamDto(Long teamId, String name, String tag, String logoUrl, Integer rating) {
	}

	public record PageResponse<T>(List<T> content, int page, int size, int totalPages, long totalElements) {
	}

}
