package com.abe.gg_stats.batch.endToEnd;

import com.abe.gg_stats.repository.jdbc.MatchIngestionDao;
import com.abe.gg_stats.service.OpenDotaApiService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@SpringBatchTest
@SpringBootTest
@ActiveProfiles("test")
public class NewMatchesJobTest {

	@MockitoBean
	private OpenDotaApiService api;

	@MockitoBean
	private MatchIngestionDao dao;

	private JsonNode makeMatch(long matchId) {
		ObjectNode n = JsonNodeFactory.instance.objectNode();
		n.put("match_id", matchId);
		return n;
	}

	@Test
	void launchJob(@Autowired JobLauncherTestUtils utils, @Autowired @Qualifier("newMatchesIngestionJob") Job job)
			throws Exception {
		utils.setJob(job);

		when(dao.getMaxMatchId()).thenReturn(100L);
		when(api.getProMatchesPage(null))
			.thenReturn(Optional.of(JsonNodeFactory.instance.arrayNode().add(makeMatch(200L))));
		when(api.getMatchDetail(200L)).thenReturn(Optional.of(makeMatch(200L)));

		JobExecution execution = utils.launchJob();

		assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
		verify(dao, atLeastOnce()).upsertMatch(any());
	}

}
