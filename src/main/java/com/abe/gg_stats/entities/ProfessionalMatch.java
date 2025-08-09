package com.abe.gg_stats.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Entity
@Table(name = "professional_match")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProfessionalMatch {

  @Id
  @Column(name = "match_id")
  private Long matchId;

  @Column(name = "start_time")
  private Instant startTime;

  @Column(name = "duration")
  private Integer duration; // seconds

  @Column(name = "radiant_win")
  private Boolean radiantWin;

  @Column(name = "radiant_team_id")
  private Long radiantTeamId;

  @Column(name = "dire_team_id")
  private Long direTeamId;

  @Column(name = "league_id")
  private Long leagueId;

  @Column(name = "patch")
  private Integer patch;
}