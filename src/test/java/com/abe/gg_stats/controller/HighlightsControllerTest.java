package com.abe.gg_stats.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.abe.gg_stats.dto.response.HighlightsDto;
import com.abe.gg_stats.service.HighlightsService;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = HighlightsController.class)
@AutoConfigureMockMvc(addFilters = false)
class HighlightsControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private HighlightsService highlightsService;

	@Test
	void testHighlights_ReturnsBadRequestWhenServiceReturnsNull() throws Exception {
		when(highlightsService.getHighlights(anyString(), any(), anyInt(), anyString(), anyInt())).thenReturn(null);

		mockMvc.perform(get("/highlights").accept(MediaType.APPLICATION_JSON)).andExpect(status().isBadRequest());
	}

	@Test
	void testHighlights_ReturnsOkWhenServiceReturnsDto() throws Exception {
		when(highlightsService.getHighlights(eq("patch"), isNull(), eq(5), eq("lift"), eq(0)))
			.thenReturn(new HighlightsDto(10L, Collections.emptyList(), Collections.emptyList()));

		mockMvc.perform(get("/highlights").accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
	}

}
