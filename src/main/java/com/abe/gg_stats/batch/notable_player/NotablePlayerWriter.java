package com.abe.gg_stats.batch.notable_player;

import com.abe.gg_stats.batch.BaseWriter;
import com.abe.gg_stats.dto.request.opendota.OpenDotaNotablePlayerDto;
import com.abe.gg_stats.dto.request.opendota.mapper.OpenDotaNotablePlayerMapper;
import com.abe.gg_stats.entity.NotablePlayer;
import com.abe.gg_stats.repository.NotablePlayerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class NotablePlayerWriter extends BaseWriter<OpenDotaNotablePlayerDto, NotablePlayer> {

	private final OpenDotaNotablePlayerMapper openDotaNotablePlayerMapper;

	@Autowired
	public NotablePlayerWriter(NotablePlayerRepository notablePlayerRepository,
			OpenDotaNotablePlayerMapper openDotaNotablePlayerMapper) {
		super(notablePlayerRepository);
		this.openDotaNotablePlayerMapper = openDotaNotablePlayerMapper;
	}

	@Override
	public NotablePlayer dtoToEntity(OpenDotaNotablePlayerDto openDotaNotablePlayerDto) {
		return openDotaNotablePlayerMapper.dtoToEntity(openDotaNotablePlayerDto);
	}

}