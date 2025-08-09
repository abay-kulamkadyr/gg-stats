package com.abe.gg_stats.entities;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Entity
@Table(name = "hero")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Hero {

  @Id
  private Long id;
  private String name;
  private String localizedName;
  private String primaryAttr;
  private String attackType;

  @ElementCollection
  private List<String> roles;
}