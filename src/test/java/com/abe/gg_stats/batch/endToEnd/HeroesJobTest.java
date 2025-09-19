package com.abe.gg_stats.batch.endToEnd;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

import com.abe.gg_stats.entity.Hero;
import com.abe.gg_stats.repository.HeroRepository;
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
@SpringBootTest()
@ActiveProfiles("test")
public class HeroesJobTest {

	private final ObjectMapper objectMapper = new ObjectMapper();

	// Mock the external API service to control its response
	@MockitoBean
	private OpenDotaApiService mockOpenDotaApiService;

	// Autowire the repository to verify that data was written
	@Autowired
	private HeroRepository heroRepository;

	@Test
	void launchHeroesJobToEnd(@Autowired JobLauncherTestUtils utils,
			@Autowired @Qualifier("heroesUpdateJob") Job heroesJob) throws Exception {

		// 1. Setup the mock API response for the test
		ArrayNode mockResponse = objectMapper.createArrayNode();
		ObjectNode heroNode1 = mockResponse.addObject();
		heroNode1.put("id", 1);
		heroNode1.put("name", "npc_dota_hero_antimage");
		heroNode1.put("localized_name", "Anti-Mage");
		heroNode1.put("primary_attr", "agi");
		heroNode1.put("attack_type", "Melee");
		heroNode1.putArray("roles").add("Carry").add("Escape");

		ObjectNode heroNode2 = mockResponse.addObject();
		heroNode2.put("id", 2);
		heroNode2.put("name", "npc_dota_hero_axe");
		heroNode2.put("localized_name", "Axe");
		heroNode2.put("primary_attr", "str");
		heroNode2.put("attack_type", "Melee");
		heroNode2.putArray("roles").add("Initiator").add("Durable");

		when(mockOpenDotaApiService.getHeroes()).thenReturn(Optional.of(mockResponse));

		// 2. Set the specific job to test and launch it
		utils.setJob(heroesJob);
		JobExecution execution = utils.launchJob();

		// 3. Verify the job's final status
		assertEquals(BatchStatus.COMPLETED, execution.getStatus(), "The batch job should complete successfully.");

		// 4. Verify that the data was correctly written to the in-memory database
		List<Hero> savedHeroes = heroRepository.findAll();
		assertEquals(2, savedHeroes.size(), "Two heroes should have been saved to the database.");

		Hero antiMage = heroRepository.findById(1).orElse(null);
		assertNotNull(antiMage);
		assertEquals("Anti-Mage", antiMage.getLocalizedName());

		Hero axe = heroRepository.findById(2).orElse(null);
		assertNotNull(axe);
		assertEquals("Axe", axe.getLocalizedName());
	}

}
