package com.abe.gg_stats.batch.player;

import com.abe.gg_stats.batch.BaseWriter;
import com.abe.gg_stats.dto.request.opendota.OpenDotaPlayerDto;
import com.abe.gg_stats.dto.request.opendota.mapper.OpenDotaPlayerMapper;
import com.abe.gg_stats.entity.Player;
import com.abe.gg_stats.repository.PlayerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class PlayerWriter extends BaseWriter<OpenDotaPlayerDto, Player> {

	private final OpenDotaPlayerMapper openDotaPlayerMapper;

	@Autowired
	public PlayerWriter(PlayerRepository playerRepository, OpenDotaPlayerMapper openDotaPlayerMapper) {
		super(playerRepository);
		this.openDotaPlayerMapper = openDotaPlayerMapper;
	}

	@Override
	protected Player dtoToEntity(OpenDotaPlayerDto playerDto) {
		return openDotaPlayerMapper.dtoToEntity(playerDto);
	}

}
