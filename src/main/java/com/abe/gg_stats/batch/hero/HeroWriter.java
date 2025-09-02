package com.abe.gg_stats.batch.hero;

import com.abe.gg_stats.batch.BaseWriter;
import com.abe.gg_stats.entity.Hero;
import com.abe.gg_stats.repository.HeroRepository;
import com.abe.gg_stats.util.LoggingConstants;
import com.abe.gg_stats.util.LoggingUtils;
import com.abe.gg_stats.util.MDCLoggingContext;
import com.abe.gg_stats.dto.HeroDto;
import com.abe.gg_stats.dto.HeroMapper;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class HeroWriter extends BaseWriter<HeroDto> {

	private final HeroRepository heroRepository;

	private final HeroMapper heroMapper;

	@Override
	protected void writeItem(HeroDto heroDto) {
		// Set up writing context
		String correlationId = MDCLoggingContext.getOrCreateCorrelationId();
		MDCLoggingContext.updateContext("operationType", LoggingConstants.OPERATION_TYPE_BATCH);
		MDCLoggingContext.updateContext("batchType", "heroes");

		try {
			Hero hero = heroMapper.dtoToEntity(heroDto);
			heroRepository.save(hero);
			LoggingUtils.logDebug("Successfully saved hero to database", "correlationId=" + correlationId,
					"heroId=" + hero.getId(), "heroName=" + hero.getName());
		}
		catch (Exception e) {
			LoggingUtils.logOperationFailure("hero database save", "Failed to save hero to database", e,
					"correlationId=" + correlationId, "heroId=" + heroDto.id(), "heroName=" + heroDto.name(),
					"errorType=" + e.getClass().getSimpleName());
			throw e;
		}
	}

	@Override
	protected String getItemTypeDescription() {
		return "hero";
	}

}
