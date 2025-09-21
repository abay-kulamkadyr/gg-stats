package com.abe.gg_stats.controller;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.abe.gg_stats.service.AggregationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = AggregationsController.class)
@AutoConfigureMockMvc(addFilters = false)
class AggregationsControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private AggregationService aggregationService;

	@Test
	void testAggregation_refreshEndpointInvokesServiceAndReturnsOk() throws Exception {
		doNothing().when(aggregationService).refreshPatchesAndAggregations();

		mockMvc.perform(post("/api/aggregations/refresh")).andExpect(status().isOk());

		verify(aggregationService).refreshPatchesAndAggregations();
	}

}
