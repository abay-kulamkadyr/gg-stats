package com.abe.gg_stats.batch.notablePlayer;

import com.abe.gg_stats.batch.BaseWriter;
import com.abe.gg_stats.entity.NotablePlayer;
import com.abe.gg_stats.repository.NotablePlayerRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class NotablePlayerWriter extends BaseWriter<NotablePlayer> {

	private final NotablePlayerRepository notablePlayerRepository;

	public NotablePlayerWriter(NotablePlayerRepository notablePlayerRepository) {
		this.notablePlayerRepository = notablePlayerRepository;
	}

	@Override
	protected void writeItem(NotablePlayer notablePlayer) {
		notablePlayerRepository.save(notablePlayer);
	}

	@Override
	protected String getItemTypeDescription() {
		return "notable player";
	}

}