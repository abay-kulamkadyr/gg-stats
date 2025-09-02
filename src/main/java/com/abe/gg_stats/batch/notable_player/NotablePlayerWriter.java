package com.abe.gg_stats.batch.notable_player;

import com.abe.gg_stats.batch.BaseWriter;
import com.abe.gg_stats.dto.NotablePlayerDto;
import com.abe.gg_stats.dto.mapper.NotablePlayerMapper;
import com.abe.gg_stats.entity.NotablePlayer;
import com.abe.gg_stats.entity.Team;
import com.abe.gg_stats.repository.NotablePlayerRepository;
import com.abe.gg_stats.repository.TeamRepository;
import org.springframework.stereotype.Component;

@Component
public class NotablePlayerWriter extends BaseWriter<NotablePlayerDto> {

	private final NotablePlayerRepository notablePlayerRepository;

	private final TeamRepository teamRepository;

	private final NotablePlayerMapper notablePlayerMapper;

	public NotablePlayerWriter(NotablePlayerRepository notablePlayerRepository, TeamRepository teamRepository,
			NotablePlayerMapper notablePlayerMapper) {
		this.notablePlayerRepository = notablePlayerRepository;
		this.teamRepository = teamRepository;
		this.notablePlayerMapper = notablePlayerMapper;
	}

	@Override
	protected void writeItem(NotablePlayerDto dto) {
		NotablePlayer entity = notablePlayerMapper.dtoToEntity(dto);
		if (dto.teamId() != null && dto.teamId() > 0) {
			teamRepository.findById(dto.teamId()).ifPresent(entity::setTeam);
		}
		notablePlayerRepository.save(entity);
	}

	@Override
	protected String getItemTypeDescription() {
		return "notable player";
	}

}