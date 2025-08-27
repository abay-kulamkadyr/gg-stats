package com.abe.gg_stats.batch.hero;

import com.abe.gg_stats.batch.BaseProcessor;
import com.abe.gg_stats.entity.Hero;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class HeroProcessor extends BaseProcessor<JsonNode, Hero> {

  @Override
  protected boolean isValidInput(@NonNull JsonNode item) {

    // Check for required fields
    if (!item.has("id") || item.get("id").isNull()) {
      log.debug("Hero data missing or null 'id' field");
      return false;
    }

    if (!item.has("name") || item.get("name").isNull()) {
      log.debug("Hero data missing or null 'name' field");
      return false;
    }

    if (!item.has("localized_name") || item.get("localized_name").isNull()) {
      log.debug("Hero data missing or null 'localized_name' field");
      return false;
    }

    // Validate ID is a positive integer
    try {
      int id = item.get("id").asInt();
      if (id <= 0) {
        log.debug("Hero ID must be positive, got: {}", id);
        return false;
      }
    } catch (Exception e) {
      log.debug("Hero ID is not a valid integer: {}", item.get("id"));
      return false;
    }

    // Validate name is not empty
    String name = item.get("name").asText();
    if (name == null || name.trim().isEmpty()) {
      log.debug("Hero name is empty or null");
      return false;
    }

    return true;
  }

  @Override
  protected Hero processItem(@NonNull JsonNode item) {
    Hero hero = new Hero();

    // Required fields
    hero.setId(item.get("id").asInt());
    hero.setName(item.get("name").asText());
    hero.setLocalizedName(item.get("localized_name").asText());

    // Optional fields
    hero.setPrimaryAttr(getTextValue(item, "primary_attr").orElse(null));
    hero.setAttackType(getTextValue(item, "attack_type").orElse(null));

    // Roles array
    List<String> roles = processRolesArray(item);
    hero.setRoles(roles);

    log.debug("Successfully processed hero: {} (ID: {})", hero.getName(), hero.getId());
    return hero;
  }

  @Override
  protected String getItemTypeDescription() {
    return "hero";
  }

  /**
   * Process roles array from JSON data
   */
  private List<String> processRolesArray(JsonNode item) {
    if (item.has("roles") && item.get("roles").isArray()) {
      return StreamSupport.stream(item.get("roles").spliterator(), false)
          .filter(role -> role != null && !role.isNull())
          .map(JsonNode::asText)
          .map(String::trim)
          .filter(s -> !s.isEmpty())
          .collect(Collectors.toList());
    }
    return Collections.emptyList();
  }

  /**
   * Get text value from JSON node, returning null if field is missing or empty
   */
  private Optional<String> getTextValue(JsonNode node, String fieldName) {
    JsonNode field = node.get(fieldName);
    if (field == null || field.isNull()) {
      return Optional.empty();
    }
    String value = field.asText().trim();
    return value.isEmpty() ? Optional.empty() : Optional.of(value);
  }

}