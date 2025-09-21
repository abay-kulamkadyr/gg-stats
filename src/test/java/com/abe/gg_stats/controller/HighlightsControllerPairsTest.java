package com.abe.gg_stats.controller;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.abe.gg_stats.dto.response.HighlightsDuoDto;
import com.abe.gg_stats.dto.response.HighlightsHeroPairsDto;
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
class HighlightsControllerPairsTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private HighlightsService highlightsService;

	@Test
	void testPairHighlights_ReturnsBadRequestWhenServiceReturnsNull() throws Exception {
		when(highlightsService.getPairHighlights(eq("synergy"), anyInt(), anyInt())).thenReturn(null);

		mockMvc
			.perform(get("/highlights/pairs").param("view", "synergy")
				.param("weekOffset", "0")
				.param("limit", "5")
				.accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isBadRequest());
	}

	@Test
	void testPairHighlights_ReturnsOkWhenServiceReturnsDto() throws Exception {
		HighlightsHeroPairsDto pair = new HighlightsHeroPairsDto(1, 2, 40, 0.8, 0.9, 1.2, 0.1, 0.2, "Hero A", "Hero B",
				"npc_dota_hero_hero_a", "npc_dota_hero_hero_b", "hero_a", "hero_b", "https://.../hero_a.png",
				"https://.../hero_b.png");
		HighlightsDuoDto dto = new HighlightsDuoDto("2025-W37", "synergy", 10L, Collections.singletonList(pair));

		when(highlightsService.getPairHighlights(eq("synergy"), anyInt(), anyInt())).thenReturn(dto);

		mockMvc
			.perform(get("/highlights/pairs").param("view", "synergy")
				.param("weekOffset", "0")
				.param("limit", "5")
				.accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk());
	}

}
