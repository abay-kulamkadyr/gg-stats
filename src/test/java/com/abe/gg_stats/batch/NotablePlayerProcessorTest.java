package com.abe.gg_stats.batch;

import com.abe.gg_stats.batch.notable_player.NotablePlayerProcessor;
import com.abe.gg_stats.entity.NotablePlayer;
import com.abe.gg_stats.entity.Team;
import com.abe.gg_stats.repository.NotablePlayerRepository;
import com.abe.gg_stats.repository.TeamRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotablePlayerProcessorTest {

	@Mock
	private NotablePlayerRepository notablePlayerRepository;

	@Mock
	private TeamRepository teamRepository;

	private NotablePlayerProcessor processor;

	private ObjectMapper objectMapper;

	@BeforeEach
	void setUp() {
		processor = new NotablePlayerProcessor(notablePlayerRepository, teamRepository);
		objectMapper = new ObjectMapper();
	}

	@Test
	void testProcess_ValidNewNotablePlayer_ShouldCreateNew() throws JsonProcessingException {
		// Given
		String validJson = """
				{
					"account_id": 12345,
					"name": "TestPlayer",
					"country_code": "US",
					"fantasy_role": 1,
					"is_locked": false,
					"is_pro": true,
					"team_id": 67890
				}
				""";
		JsonNode item = objectMapper.readTree(validJson);

		Team team = new Team();
		team.setTeamId(67890L);
		team.setName("TestTeam");

		when(notablePlayerRepository.findById(12345L)).thenReturn(Optional.empty());
		when(teamRepository.findById(67890L)).thenReturn(Optional.of(team));

		// When
		NotablePlayer result = processor.process(item);

		// Then
		assertNotNull(result);
		assertEquals(12345L, result.getAccountId());
		assertEquals("TestPlayer", result.getName());
		assertEquals("US", result.getCountryCode());
		assertEquals(1, result.getFantasyRole());
		assertFalse(result.getIsLocked());
		assertTrue(result.getIsPro());
		assertEquals(team, result.getTeam());

		verify(notablePlayerRepository).findById(12345L);
		verify(teamRepository).findById(67890L);
	}

	@Test
	void testProcess_ValidExistingNotablePlayer_ShouldUpdate() throws JsonProcessingException {
		// Given
		String validJson = """
				{
					"account_id": 12345,
					"name": "UpdatedPlayer",
					"country_code": "CA",
					"fantasy_role": 2,
					"is_locked": true,
					"is_pro": false
				}
				""";
		JsonNode item = objectMapper.readTree(validJson);

		NotablePlayer existingPlayer = new NotablePlayer();
		existingPlayer.setAccountId(12345L);
		existingPlayer.setName("OldName");

		when(notablePlayerRepository.findById(12345L)).thenReturn(Optional.of(existingPlayer));

		// When
		NotablePlayer result = processor.process(item);

		// Then
		assertNotNull(result);
		assertEquals(12345L, result.getAccountId());
		assertEquals("UpdatedPlayer", result.getName());
		assertEquals("CA", result.getCountryCode());
		assertEquals(2, result.getFantasyRole());
		assertTrue(result.getIsLocked());
		assertFalse(result.getIsPro());

		verify(notablePlayerRepository).findById(12345L);
		verify(teamRepository, never()).findById(any());
	}

	@Test
	void testProcess_NullItem_ShouldThrowException() {
		// When & Then - BaseProcessor.process has @NonNull annotation, so it should throw
		// NullPointerException
		assertThrows(NullPointerException.class, () -> processor.process(null));
		verify(notablePlayerRepository, never()).findById(any());
		verify(teamRepository, never()).findById(any());
	}

	@Test
	void testProcess_MissingAccountId_ShouldReturnNull() throws JsonProcessingException {
		// Given
		String invalidJson = """
				{
					"name": "TestPlayer",
					"country_code": "US"
				}
				""";
		JsonNode item = objectMapper.readTree(invalidJson);

		// When
		NotablePlayer result = processor.process(item);

		// Then
		assertNull(result);
		verify(notablePlayerRepository, never()).findById(any());
	}

	@Test
	void testProcess_NullAccountId_ShouldReturnNull() throws JsonProcessingException {
		// Given
		String invalidJson = """
				{
					"account_id": null,
					"name": "TestPlayer"
				}
				""";
		JsonNode item = objectMapper.readTree(invalidJson);

		// When
		NotablePlayer result = processor.process(item);

		// Then
		assertNull(result);
		verify(notablePlayerRepository, never()).findById(any());
	}

	@Test
	void testProcess_InvalidAccountIdType_ShouldReturnNull() throws JsonProcessingException {
		// Given
		String invalidJson = """
				{
					"account_id": "invalid_id",
					"name": "TestPlayer"
				}
				""";
		JsonNode item = objectMapper.readTree(invalidJson);

		// When
		NotablePlayer result = processor.process(item);

		// Then
		assertNull(result);
		verify(notablePlayerRepository, never()).findById(any());
	}

	@Test
	void testProcess_NegativeAccountId_ShouldReturnNull() throws JsonProcessingException {
		// Given
		String invalidJson = """
				{
					"account_id": -12345,
					"name": "TestPlayer"
				}
				""";
		JsonNode item = objectMapper.readTree(invalidJson);

		// When
		NotablePlayer result = processor.process(item);

		// Then
		assertNull(result);
		verify(notablePlayerRepository, never()).findById(any());
	}

	@Test
	void testProcess_ZeroAccountId_ShouldReturnNull() throws JsonProcessingException {
		// Given
		String invalidJson = """
				{
					"account_id": 0,
					"name": "TestPlayer"
				}
				""";
		JsonNode item = objectMapper.readTree(invalidJson);

		// When
		NotablePlayer result = processor.process(item);

		// Then
		assertNull(result);
		verify(notablePlayerRepository, never()).findById(any());
	}

	@Test
	void testProcess_WithTeamId_ShouldAssociateTeam() throws JsonProcessingException {
		// Given
		String validJson = """
				{
					"account_id": 12345,
					"name": "TestPlayer",
					"team_id": 67890
				}
				""";
		JsonNode item = objectMapper.readTree(validJson);

		Team team = new Team();
		team.setTeamId(67890L);
		team.setName("TestTeam");

		when(notablePlayerRepository.findById(12345L)).thenReturn(Optional.empty());
		when(teamRepository.findById(67890L)).thenReturn(Optional.of(team));

		// When
		NotablePlayer result = processor.process(item);

		// Then
		assertNotNull(result);
		assertEquals(team, result.getTeam());
		verify(teamRepository).findById(67890L);
	}

	@Test
	void testProcess_WithInvalidTeamId_ShouldHandleGracefully() throws JsonProcessingException {
		// Given
		String validJson = """
				{
					"account_id": 12345,
					"name": "TestPlayer",
					"team_id": "invalid_team_id"
				}
				""";
		JsonNode item = objectMapper.readTree(validJson);

		when(notablePlayerRepository.findById(12345L)).thenReturn(Optional.empty());

		// When
		NotablePlayer result = processor.process(item);

		// Then
		assertNotNull(result);
		assertNull(result.getTeam());
		verify(teamRepository, never()).findById(any());
	}

	@Test
	void testProcess_WithNegativeTeamId_ShouldHandleGracefully() throws JsonProcessingException {
		// Given
		String validJson = """
				{
					"account_id": 12345,
					"name": "TestPlayer",
					"team_id": -67890
				}
				""";
		JsonNode item = objectMapper.readTree(validJson);

		when(notablePlayerRepository.findById(12345L)).thenReturn(Optional.empty());

		// When
		NotablePlayer result = processor.process(item);

		// Then
		assertNotNull(result);
		assertNull(result.getTeam());
		verify(teamRepository, never()).findById(any());
	}

	@Test
	void testProcess_WithNullTeamId_ShouldHandleGracefully() throws JsonProcessingException {
		// Given
		String validJson = """
				{
					"account_id": 12345,
					"name": "TestPlayer",
					"team_id": null
				}
				""";
		JsonNode item = objectMapper.readTree(validJson);

		when(notablePlayerRepository.findById(12345L)).thenReturn(Optional.empty());

		// When
		NotablePlayer result = processor.process(item);

		// Then
		assertNotNull(result);
		assertNull(result.getTeam());
		verify(teamRepository, never()).findById(any());
	}

	@Test
	void testProcess_WithMissingFields_ShouldSetDefaults() throws JsonProcessingException {
		// Given
		String validJson = """
				{
					"account_id": 12345
				}
				""";
		JsonNode item = objectMapper.readTree(validJson);

		when(notablePlayerRepository.findById(12345L)).thenReturn(Optional.empty());

		// When
		NotablePlayer result = processor.process(item);

		// Then
		assertNotNull(result);
		assertEquals(12345L, result.getAccountId());
		assertNull(result.getName());
		assertNull(result.getCountryCode());
		assertNull(result.getFantasyRole());
		assertFalse(result.getIsLocked());
		assertTrue(result.getIsPro());
		assertNull(result.getTeam());
	}

	@Test
	void testProcess_WithRepositoryException_ShouldThrowException() {
		// Given
		String validJson = """
				{
					"account_id": 12345,
					"name": "TestPlayer"
				}
				""";
		JsonNode item;
		try {
			item = objectMapper.readTree(validJson);
		}
		catch (Exception e) {
			fail("Failed to parse test JSON");
			return;
		}

		when(notablePlayerRepository.findById(12345L)).thenThrow(new RuntimeException("Database error"));

		// When & Then - Should throw the repository exception
		assertThrows(RuntimeException.class, () -> processor.process(item));

		verify(notablePlayerRepository).findById(12345L);
	}

}
