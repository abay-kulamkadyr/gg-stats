package com.abe.gg_stats.batch.player;

import com.abe.gg_stats.batch.BaseWriter;
import com.abe.gg_stats.dto.PlayerDto;
import com.abe.gg_stats.dto.mapper.PlayerMapper;
import com.abe.gg_stats.entity.Player;
import com.abe.gg_stats.repository.PlayerRepository;
import com.abe.gg_stats.util.LoggingConstants;
import com.abe.gg_stats.util.LoggingUtils;
import com.abe.gg_stats.util.MDCLoggingContext;
import org.springframework.stereotype.Component;

@Component
public class PlayerWriter extends BaseWriter<PlayerDto> {

	private final PlayerRepository playerRepository;

	private final PlayerMapper playerMapper;

	public PlayerWriter(PlayerRepository playerRepository, PlayerMapper playerMapper) {
		this.playerRepository = playerRepository;
		this.playerMapper = playerMapper;
	}

	@Override
	protected void writeItem(PlayerDto playerDto) {
		// Set up writing context
		String correlationId = MDCLoggingContext.getOrCreateCorrelationId();
		MDCLoggingContext.updateContext("operationType", LoggingConstants.OPERATION_TYPE_BATCH);
		MDCLoggingContext.updateContext("batchType", "players");

		// Data is already persisted by PlayerUpdateService in the processor
		// This writer just logs completion for consistency with other batch jobs
		try {
			if (playerDto.accountId() == null) {
				LoggingUtils.logWarning("Skipping player save due to missing accountId",
						"correlationId=" + correlationId, "playerName=" + playerDto.personName());
				return;
			}
			Player player = playerMapper.dtoToEntity(playerDto);
			playerRepository.save(player);
			LoggingUtils.logDebug("Player data has been saved", "correlationId=" + correlationId,
					"accountId=" + player.getAccountId(), "playerName=" + player.getPersonName());
		}
		catch (Exception e) {
			LoggingUtils.logOperationFailure("player database save", "Failed to save player to database", e,
					"correlationId=" + correlationId, "accountId=" + playerDto.accountId(),
					"playerName=" + playerDto.personName(), "errorType=" + e.getClass().getSimpleName());
			throw e; // Re-throw to let BaseWriter handle the error
		}
	}

	@Override
	protected String getItemTypeDescription() {
		return "player account ID (already persisted)";
	}

}
