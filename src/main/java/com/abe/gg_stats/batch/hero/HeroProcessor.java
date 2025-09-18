package com.abe.gg_stats.batch.hero;

import com.abe.gg_stats.batch.BaseProcessor;
import com.abe.gg_stats.dto.request.opendota.OpenDotaHeroDto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Component
public class HeroProcessor extends BaseProcessor<OpenDotaHeroDto> {

	private static final String localizedNameLabel = "localized_name";

	private final ObjectMapper objectMapper;

	@Autowired
	public HeroProcessor(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	@Override
	protected boolean isValidInput(@NonNull JsonNode item) {
		if (!item.has("id") || item.get("id").isNull()) {
			return false;
		}

		if (!item.has("name") || item.get("name").isNull()) {
			return false;
		}

		if (!item.has(localizedNameLabel) || item.get(localizedNameLabel).isNull()) {
			return false;
		}

		try {
			int id = item.get("id").asInt();
			if (id <= 0) {
				return false;
			}
		}
		catch (Exception e) {
			return false;
		}

		String name = item.get("name").asText();
		return name != null && !name.trim().isEmpty();
	}

	@Override
	protected OpenDotaHeroDto processItem(@NonNull JsonNode item) {
		OpenDotaHeroDto dto;

		try {
			dto = objectMapper.treeToValue(item, OpenDotaHeroDto.class);
		}
		catch (Exception e) {
			return null;
		}

		// Normalize roles: ensure non-null, trim and remove blanks/nulls
		List<String> roles = dto.roles() == null ? java.util.Collections.emptyList()
				: dto.roles()
					.stream()
					.filter(Objects::nonNull)
					.map(String::trim)
					.filter(s -> !s.isEmpty())
					.collect(java.util.stream.Collectors.toList());

		dto = new OpenDotaHeroDto(dto.id(), dto.name(), dto.localizedName(), dto.primaryAttr(), dto.attackType(),
				roles);

		return dto;
	}

}