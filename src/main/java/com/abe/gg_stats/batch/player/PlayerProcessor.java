package com.abe.gg_stats.batch.player;

import com.abe.gg_stats.batch.BaseProcessor;
import com.abe.gg_stats.service.PlayerUpdateService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class PlayerProcessor extends BaseProcessor<Long, Long> {

	private final PlayerUpdateService playerUpdateService;

	public PlayerProcessor(PlayerUpdateService playerUpdateService) {
		this.playerUpdateService = playerUpdateService;
	}

	@Override
	protected boolean isValidInput(Long accountId) {
		return accountId != null && accountId > 0;
	}

	@Override
	protected Long processItem(Long accountId) throws Exception {
		try {
			log.info("Processing account_id: {}", accountId);
			playerUpdateService.updatePlayerInfo(accountId);
			log.info("Successfully processed account_id: {}", accountId);
			return accountId; // Return the same ID to maintain flow
		}
		catch (PlayerUpdateService.PlayerProcessingException e) {
			log.error("Failed to process player account_id {}: {}", accountId, e.getMessage());
			throw e;
		}
		catch (Exception e) {
			log.error("Unexpected error processing account_id {}: {}", accountId, e.getMessage(), e);
			throw new PlayerProcessingException("Failed to process player: " + accountId, e);
		}
	}

	@Override
	protected String getItemTypeDescription() {
		return "player account ID";
	}

	/**
	 * Custom exception for player processing errors
	 */
	public static class PlayerProcessingException extends Exception {

		public PlayerProcessingException(String message) {
			super(message);
		}

		public PlayerProcessingException(String message, Throwable cause) {
			super(message, cause);
		}

	}

}