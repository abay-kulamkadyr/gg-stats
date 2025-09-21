package com.abe.gg_stats.config;

import static java.time.LocalTime.now;
import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.Test;

class JacksonConfigTest {

	@Test
	void objectMapperUsesSnakeCaseAndIgnoresUnknown() throws Exception {
		JacksonConfig cfg = new JacksonConfig();
		ObjectMapper mapper = cfg.objectMapper();

		// Snake case: property myValue -> my_value
		record SampleDto(String myValue) {
		}

		String json = "{\"my_value\":\"ok\",\"unknown_field\":123}";
		SampleDto dto = mapper.readValue(json, SampleDto.class);
		assertEquals("ok", dto.myValue());

		// Unknown properties do not fail
		String withUnknown = "{\"my_value\":\"ok\",\"extra\":\"ignored\"}";
		assertDoesNotThrow(() -> mapper.readValue(withUnknown, SampleDto.class));

		// JavaTimeModule registered: serialize OffsetDateTime without exception
		Map<String, Object> sample = Map.of("time", now());
		String serialized = mapper.writeValueAsString(sample);
		JsonNode node = mapper.readTree(serialized);
		assertTrue(node.has("time"));
	}

}
