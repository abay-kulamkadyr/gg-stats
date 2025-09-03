package com.abe.gg_stats.batch;

import com.abe.gg_stats.batch.notable_player.NotablePlayerProcessor;
import com.abe.gg_stats.config.JacksonConfig;
import com.abe.gg_stats.dto.NotablePlayerDto;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class NotablePlayerProcessorTest {

	private NotablePlayerProcessor processor;

	private ObjectMapper objectMapper;

	@BeforeEach
	void setUp() {
		objectMapper = new JacksonConfig().objectMapper();
		processor = new NotablePlayerProcessor(objectMapper);
	}

	@Test
	void testProcess_ValidNewNotablePlayer_ShouldCreateDto() throws JsonProcessingException {
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

		NotablePlayerDto result = processor.process(item);

		assertNotNull(result);
		assertEquals(12345L, result.accountId());
		assertEquals("TestPlayer", result.name());
		assertEquals("US", result.countryCode());
		assertEquals(1, result.fantasyRole());
		assertEquals(false, result.isLocked());
		assertEquals(true, result.isPro());
		assertEquals(67890L, result.teamId());
	}

	@Test
	void testProcess_NullItem_ShouldThrowException() {
		assertThrows(NullPointerException.class, () -> processor.process(null));
	}

	@Test
	void testProcess_MissingAccountId_ShouldReturnNull() throws JsonProcessingException {
		String invalidJson = """
				{
					"name": "TestPlayer",
					"country_code": "US"
				}
				""";
		boolean isValid = processor.isValidInput(objectMapper.readTree(invalidJson));
		assertFalse(isValid);
	}

	@Test
	void testProcess_InvalidTeamId_ShouldReturnDtoWithNullTeam() throws JsonProcessingException {
		String validJson = """
				{
					"account_id": 12345,
					"name": "TestPlayer",
					"team_id": "invalid_team_id"
				}
				""";
		JsonNode item = objectMapper.readTree(validJson);

		NotablePlayerDto result = processor.process(item);
		assertNotNull(result);
		assertNull(result.teamId());
	}

	@Test
	void testProcess_WithMissingFields_ShouldSetDefaults() throws JsonProcessingException {
		String validJson = """
				{
					"account_id": 12345
				}
				""";
		JsonNode item = objectMapper.readTree(validJson);

		NotablePlayerDto result = processor.process(item);
		assertNotNull(result);
		assertEquals(12345L, result.accountId());
		assertNull(result.name());
		assertNull(result.countryCode());
		assertNull(result.fantasyRole());
		assertNull(result.isLocked());
		assertNull(result.isPro());
		assertNull(result.teamId());
	}

}
