package com.abe.gg_stats.batch.endToEnd;

import com.abe.gg_stats.entity.Hero;
import com.abe.gg_stats.entity.HeroRanking;
import com.abe.gg_stats.repository.HeroRankingRepository;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBatchTest
@SpringBootTest
@ActiveProfiles("test")
public class HeroRankingsJobTest {

	private final ObjectMapper objectMapper = new ObjectMapper();

	@MockitoBean
	private OpenDotaApiService mockOpenDotaApiService;

	@Autowired
	private HeroRepository heroRepository;

	@Autowired
	private HeroRankingRepository heroRankingRepository;

	@Test
	void launchHeroRankingsJob(@Autowired JobLauncherTestUtils utils,
			@Autowired @Qualifier("heroRankingUpdateJob") Job heroRankingsJob) throws Exception {

		// 1. Prepare prerequisite hero in DB
		Hero hero = new Hero();
		hero.setId(1);
		hero.setName("npc_dota_hero_antimage");
		hero.setLocalizedName("Anti-Mage");
		hero.setPrimaryAttr("agi");
		hero.setAttackType("Melee");
		hero.setRoles(List.of("Carry", "Escape"));
		heroRepository.save(hero);

		// 2. Prepare mock API response for hero ranking
		ObjectNode rootNode = objectMapper.createObjectNode();
		rootNode.put("hero_id", 1);

		ArrayNode rankings = objectMapper.createArrayNode();
		ObjectNode ranking1 = rankings.addObject();
		ranking1.put("account_id", 12345L);
		ranking1.put("score", 99.9);

		ObjectNode ranking2 = rankings.addObject();
		ranking2.put("account_id", 67890L);
		ranking2.put("score", 88.8);

		rootNode.set("rankings", rankings);

		when(mockOpenDotaApiService.getHeroRanking(1)).thenReturn(Optional.of(rootNode));

		// 3. Launch the job
		utils.setJob(heroRankingsJob);
		JobExecution execution = utils.launchJob();

		// 4. Verify status
		assertEquals(BatchStatus.COMPLETED, execution.getStatus(),
				"Hero rankings batch job should complete successfully.");

		// 5. Verify persisted hero rankings
		List<HeroRanking> rankingsInDb = heroRankingRepository.findAll();
		assertEquals(2, rankingsInDb.size(), "Two rankings should have been persisted.");

		HeroRanking r1 = rankingsInDb.stream().filter(r -> r.getAccountId().equals(12345L)).findFirst().orElse(null);
		assertNotNull(r1);
		assertEquals(99.9, r1.getScore());

		HeroRanking r2 = rankingsInDb.stream().filter(r -> r.getAccountId().equals(67890L)).findFirst().orElse(null);
		assertNotNull(r2);
		assertEquals(88.8, r2.getScore());
	}

}
