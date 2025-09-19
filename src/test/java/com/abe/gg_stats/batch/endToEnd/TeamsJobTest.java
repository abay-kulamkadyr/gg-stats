package com.abe.gg_stats.batch.endToEnd;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

import com.abe.gg_stats.entity.Team;
import com.abe.gg_stats.repository.TeamRepository;
import com.abe.gg_stats.service.OpenDotaApiService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
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
class TeamsJobTest {

	private final ObjectMapper objectMapper = new ObjectMapper();

	@MockitoBean
	private OpenDotaApiService openDotaApiService;

	@Autowired
	private TeamRepository teamRepository;

	@Test
	void launchTeamsJobToEnd(@Autowired JobLauncherTestUtils utils,
			@Autowired @Qualifier("teamsUpdateJob") Job teamsJob) throws Exception {

		// 1. Prepare mock OpenDota API response (page 0)
		ArrayNode mockTeams = objectMapper.createArrayNode();
		ObjectNode teamNode = mockTeams.addObject();
		teamNode.put("team_id", "777");
		teamNode.put("name", "Radiant Champs");
		teamNode.put("tag", "RCH");

		when(openDotaApiService.getTeamsPage(0)).thenReturn(Optional.of(mockTeams));

		// 2. Run job
		utils.setJob(teamsJob);
		JobExecution execution = utils.launchJob();

		// 3. Assert job success
		assertEquals(BatchStatus.COMPLETED, execution.getStatus());

		// 4. Verify DB changes
		List<Team> savedTeams = teamRepository.findAll();
		assertEquals(1, savedTeams.size());

		Team saved = teamRepository.findById(777L).orElse(null);
		assertNotNull(saved);
		assertEquals("Radiant Champs", saved.getName());
	}

}
