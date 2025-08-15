package com.abe.gg_stats.batch;

import com.abe.gg_stats.service.PlayerUpdateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class PlayerProcessor implements ItemProcessor<Long, Long> {

	private final PlayerUpdateService playerUpdateService;

	@Override
	public Long process(Long accountId) throws Exception {
		try {
			// Validate input
			if (accountId == null) {
				log.warn("Received null account_id, skipping");
				return null;
			}

			if (accountId <= 0) {
				log.warn("Invalid account_id: {} (must be positive)", accountId);
				return null;
			}

			log.info("Processing account_id: {}", accountId);

			playerUpdateService.updatePlayerInfo(accountId);

			log.info("Successfully processed account_id: {}", accountId);
			return accountId; // Return the same ID to maintain flow

		}
		catch (PlayerUpdateService.PlayerProcessingException e) {
			log.error("Failed to process player account_id {}: {}", accountId, e.getMessage());
			// Re-throw to trigger retry/skip logic
			throw e;
		}
		catch (Exception e) {
			log.error("Unexpected error processing account_id {}: {}", accountId, e.getMessage(), e);
			// Re-throw to trigger retry/skip logic
			throw new PlayerProcessingException("Failed to process player: " + accountId, e);
		}
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