package com.abe.gg_stats;

import com.abe.gg_stats.entities.Hero;
import com.abe.gg_stats.entities.ProfessionalMatch;
import com.abe.gg_stats.repositories.HeroRepository;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@EnableBatchProcessing
@RequiredArgsConstructor
public class BatchConfig {

  private final JobRepository jobRepository;
  private final PlatformTransactionManager transactionManager;
  private final HeroRepository heroRepository;
  private final OpenDotaClient openDotaClient;

  @Bean
  public Job importDotaDataJob() {
    return new JobBuilder("importDotaDataJob", jobRepository)
        .start(importHeroesStep())
        .next(importProMatchesStep())
        .build();
  }

  @Bean
  public Step importHeroesStep() {
    return new StepBuilder("importHeroesStep", jobRepository)
        .<Map<String, Object>, Hero>chunk(10, transactionManager)
        .reader(heroItemReader())
        .processor(heroItemProcessor())
        .writer(heroItemWriter())
        .build();
  }

  @Bean
  public ItemReader<Map<String, Object>> heroItemReader() {
    return new ListItemReader<>(openDotaClient.getHeroes());
  }

  @Bean
  public ItemProcessor<Map<String, Object>, Hero> heroItemProcessor() {
    return item -> new Hero(
        ((Number)item.get("id")).longValue(),
        (String) item.get("name"),
        (String) item.get("localized_name"),
        (String) item.get("primary_attr"),
        (String) item.get("attack_type"),
        (List<String>) item.get("roles")
    );
  }

  @Bean
  public ItemWriter<Hero> heroItemWriter() {
    return items -> heroRepository.saveAll(items);
  }

  @Bean
  public Step importProMatchesStep() {
    return new StepBuilder("importProMatchesStep", jobRepository)
        .<Map<String, Object>, ProfessionalMatch>chunk(10, transactionManager)
        .reader(proMatchItemReader())
        .processor(proMatchItemProcessor())
        .writer(proMatchItemWriter())
        .build();
  }

  @Bean
  public ItemReader<Map<String, Object>> proMatchItemReader() {
    return new ListItemReader<>(openDotaClient.getProMatches());
  }

  @Bean
  public ItemProcessor<Map<String, Object>, ProfessionalMatch> proMatchItemProcessor() {
    return item -> new ProfessionalMatch(
        ((Number) item.get("match_id")).longValue(),
        Instant.ofEpochSecond(((Number)item.get("start_time")).longValue())
            .atZone(ZoneId.systemDefault()).toLocalDateTime(),
        ((Number)item.get("duration")).intValue(),
        (Boolean) item.get("radiant_win"),
        (String) item.get("radiant_name"),
        (String) item.get("dire_name")
    );
  }

  @Bean
  public ItemWriter<ProfessionalMatch> proMatchItemWriter() {
    return items -> {
      // save matches (similar to heroRepository)
    };
  }
}
