package com.abe.gg_stats.batch.endToEnd;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import com.abe.gg_stats.entity.Player;
import com.abe.gg_stats.repository.PlayerRepository;
import com.abe.gg_stats.service.OpenDotaApiService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBatchTest
@SpringBootTest
@ActiveProfiles("test")
class PlayersJobTest {

	private final ObjectMapper objectMapper = new ObjectMapper();

	@MockitoBean
	private OpenDotaApiService openDotaApiService;

	@Autowired
	private PlayerRepository playerRepository;

	@Test
	void launchPlayersJobToEnd(@Autowired JobLauncherTestUtils utils,
			@Autowired @Qualifier("playerUpdateJob") Job playersJob) throws Exception {

		// 1. Insert dummy player row to satisfy findAllIds()
		Player p = new Player();
		p.setAccountId(12345L);
		playerRepository.save(p);

		// 2. Prepare mock API response
		ObjectNode playerNode = objectMapper.createObjectNode();
		playerNode.put("account_id", 12345);

		ObjectNode profileNode = playerNode.putObject("profile");
		profileNode.put("steamid", 129039443);
		profileNode.put("personaname", "TestPlayer");

		when(openDotaApiService.getPlayer(12345L)).thenReturn(Optional.of(playerNode));

		// 3. Run job
		utils.setJob(playersJob);
		JobExecution execution = utils.launchJob();

		// 4. Verify job status
		assertEquals(BatchStatus.COMPLETED, execution.getStatus());

		// 5. Verify DB changes
		assertEquals(1, playerRepository.findAll().size());
		Player saved = playerRepository.findByAccountId(12345L).orElseThrow();
		// assertEquals("TestPlayer", saved.getPersonName());
	}

}
