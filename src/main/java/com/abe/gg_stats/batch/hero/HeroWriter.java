package com.abe.gg_stats.batch.hero;

import com.abe.gg_stats.batch.BaseWriter;
import com.abe.gg_stats.entity.Hero;
import com.abe.gg_stats.repository.HeroRepository;
import com.abe.gg_stats.util.LoggingConstants;
import com.abe.gg_stats.util.LoggingUtils;
import com.abe.gg_stats.util.MDCLoggingContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class HeroWriter extends BaseWriter<Hero> {

	private final HeroRepository heroRepository;

	@Autowired
	public HeroWriter(HeroRepository heroRepository) {
		this.heroRepository = heroRepository;
	}

	@Override
	protected void writeItem(Hero hero) {
		// Set up writing context
		String correlationId = MDCLoggingContext.getOrCreateCorrelationId();
		MDCLoggingContext.updateContext("operationType", LoggingConstants.OPERATION_TYPE_BATCH);
		MDCLoggingContext.updateContext("batchType", "heroes");
		
		try {
			heroRepository.save(hero);
			LoggingUtils.logDebug("Successfully saved hero to database", 
				"correlationId=" + correlationId,
				"heroId=" + hero.getId(),
				"heroName=" + hero.getName());
		} catch (Exception e) {
			LoggingUtils.logOperationFailure("hero database save", "Failed to save hero to database", e,
				"correlationId=" + correlationId,
				"heroId=" + hero.getId(),
				"heroName=" + hero.getName(),
				"errorType=" + e.getClass().getSimpleName());
			throw e; // Re-throw to let BaseWriter handle the error
		}
	}

	@Override
	protected String getItemTypeDescription() {
		return "hero";
	}

}
