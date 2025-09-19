package com.abe.gg_stats.batch.endToEnd;

import com.abe.gg_stats.config.batch.BatchExpirationProperties;
import com.abe.gg_stats.repository.jdbc.MatchIngestionDao;
import com.abe.gg_stats.service.OpenDotaApiService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
public class HistoricalMatchesJobTest {

	@MockitoBean
	private OpenDotaApiService api;

	@MockitoBean
	private MatchIngestionDao dao;

	@Autowired
	ObjectMapper objectMapper;

	@Autowired
	BatchExpirationProperties batchExpirationProperties;

	private JsonNode makeMatch() {
		ObjectNode n = JsonNodeFactory.instance.objectNode();
		n.put("match_id", 123L);
		return n;
	}

	@Test
	void launchJob(@Autowired JobLauncherTestUtils utils,
			@Autowired @Qualifier("historicalMatchesIngestionJob") Job job) throws Exception {
		utils.setJob(job);

		when(dao.getMinMatchId()).thenReturn(123L);
		when(api.getProMatchesPage(123L))
			.thenReturn(Optional.of(JsonNodeFactory.instance.arrayNode().add(makeMatch())));

		when(api.getMatchDetail(123L)).thenReturn(Optional.of(objectMapper.readTree("{\"match_id\":123}")));

		when(api.getMatchDetail(122L)).thenReturn(Optional.of(objectMapper.readTree("{\"match_id\":122}")));

		JobExecution execution = utils.launchJob();

		assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
		verify(dao, atLeastOnce()).upsertMatch(any());
	}

}
