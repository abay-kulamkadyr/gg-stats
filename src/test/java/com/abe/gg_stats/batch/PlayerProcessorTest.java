package com.abe.gg_stats.batch;

import com.abe.gg_stats.batch.player.PlayerProcessor;
import com.abe.gg_stats.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlayerProcessorTest {

	@Mock
	private PlayerUpdateService playerUpdateService;

	private PlayerProcessor playerProcessor;

	@BeforeEach
	void setUp() {
		playerProcessor = new PlayerProcessor(playerUpdateService);
	}

	@Test
	void testProcess_ValidAccountId_ShouldSucceed() throws Exception {
		// Given
		Long accountId = 12345L;
		Player expectedPlayer = new Player();
		expectedPlayer.setAccountId(accountId);
		expectedPlayer.setPersonName("TestPlayer");

		when(playerUpdateService.updatePlayerInfo(accountId)).thenReturn(expectedPlayer);

		// When
		Long result = playerProcessor.process(accountId);

		// Then
		assertNotNull(result);
		assertEquals(accountId, result);
		verify(playerUpdateService).updatePlayerInfo(accountId);
	}

	@Test
	void testProcess_PlayerServiceException_ShouldRethrow() {
		// Given
		Long accountId = 12345L;
		PlayerUpdateService.PlayerProcessingException serviceException = new PlayerUpdateService.PlayerProcessingException(
				"Failed to update player");
		when(playerUpdateService.updatePlayerInfo(accountId)).thenThrow(serviceException);

		// When & Then
		PlayerUpdateService.PlayerProcessingException exception = assertThrows(
				PlayerUpdateService.PlayerProcessingException.class, () -> playerProcessor.process(accountId));
		assertEquals("Failed to update player", exception.getMessage());
		verify(playerUpdateService).updatePlayerInfo(accountId);
	}

	@Test
	void testProcess_UnexpectedException_ShouldWrapInCustomException() {
		// Given
		Long accountId = 12345L;
		RuntimeException unexpectedException = new RuntimeException("Unexpected error");
		when(playerUpdateService.updatePlayerInfo(accountId)).thenThrow(unexpectedException);

		// When & Then
		PlayerProcessor.PlayerProcessingException exception = assertThrows(
				PlayerProcessor.PlayerProcessingException.class, () -> playerProcessor.process(accountId));
		assertEquals("Failed to process player: 12345", exception.getMessage());
		assertEquals(unexpectedException, exception.getCause());
		verify(playerUpdateService).updatePlayerInfo(accountId);
	}

	@Test
	void testProcess_NullAccountId_ShouldHandleGracefully() throws Exception {
		// Given - null account ID

		// When
		Long result = playerProcessor.process(null);

		// Then
		assertNull(result);
		verify(playerUpdateService, never()).updatePlayerInfo(any());
	}

	@Test
	void testProcess_InvalidAccountId_ShouldHandleGracefully() throws Exception {
		// Given - negative account ID
		Long invalidAccountId = -1L;

		// When
		Long result = playerProcessor.process(invalidAccountId);

		// Then
		assertNull(result);
		verify(playerUpdateService, never()).updatePlayerInfo(any());
	}

}
