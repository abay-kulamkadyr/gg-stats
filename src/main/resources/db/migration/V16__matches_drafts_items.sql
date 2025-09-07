-- Core matches
CREATE TABLE IF NOT EXISTS matches (
  match_id BIGINT PRIMARY KEY,
  start_time INT,
  duration INT,
  pre_game_duration INT,
  radiant_win BOOLEAN,
  league_id BIGINT,
  series_id BIGINT,
  series_type INT,
  cluster INT,
  lobby_type INT,
  game_mode INT,
  engine INT,
  radiant_score INT,
  dire_score INT,
  tower_status_radiant INT,
  tower_status_dire INT,
  barracks_status_radiant INT,
  barracks_status_dire INT,
  first_blood_time INT,
  radiant_team_id BIGINT,
  radiant_name TEXT,
  dire_team_id BIGINT,
  dire_name TEXT,
  radiant_captain BIGINT,
  dire_captain BIGINT,
  patch INT,
  region INT,
  replay_url TEXT,
  created_at TIMESTAMPTZ DEFAULT now(),
  updated_at TIMESTAMPTZ DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_matches_start_time ON matches(start_time);
CREATE INDEX IF NOT EXISTS idx_matches_patch ON matches(patch);
CREATE INDEX IF NOT EXISTS idx_matches_league ON matches(league_id);

-- Team ↔ match side mapping
CREATE TABLE IF NOT EXISTS team_match (
  team_id BIGINT NOT NULL,
  match_id BIGINT NOT NULL REFERENCES matches(match_id) ON DELETE CASCADE,
  radiant BOOLEAN NOT NULL,
  PRIMARY KEY (team_id, match_id)
);

-- Final picks/bans
CREATE TABLE IF NOT EXISTS picks_bans (
  match_id BIGINT NOT NULL REFERENCES matches(match_id) ON DELETE CASCADE,
  ord SMALLINT NOT NULL,
  is_pick BOOLEAN NOT NULL,
  hero_id INT,
  team SMALLINT CHECK (team IN (0,1)),
  player_slot SMALLINT,
  PRIMARY KEY (match_id, ord)
);

CREATE INDEX IF NOT EXISTS idx_picks_bans_match_pick ON picks_bans(match_id, is_pick, team);

-- Draft timings (optional)
CREATE TABLE IF NOT EXISTS draft_timings (
  match_id BIGINT NOT NULL REFERENCES matches(match_id) ON DELETE CASCADE,
  ord SMALLINT NOT NULL,
  is_pick BOOLEAN NOT NULL,
  active_team SMALLINT,
  hero_id INT,
  player_slot SMALLINT,
  extra_time INT,
  total_time_taken INT,
  PRIMARY KEY (match_id, ord)
);

-- Per-player summary
CREATE TABLE IF NOT EXISTS player_matches (
  match_id BIGINT NOT NULL REFERENCES matches(match_id) ON DELETE CASCADE,
  account_id BIGINT NOT NULL,
  player_slot INT NOT NULL,
  hero_id INT NOT NULL,
  is_radiant BOOLEAN GENERATED ALWAYS AS (player_slot < 128) STORED,
  win BOOLEAN,
  kills INT,
  deaths INT,
  assists INT,
  level INT,
  net_worth INT,
  gold_per_min INT,
  xp_per_min INT,
  lane INT,
  lane_role INT,
  is_roaming BOOLEAN,
  PRIMARY KEY (match_id, account_id)
);

CREATE INDEX IF NOT EXISTS idx_player_matches_match ON player_matches(match_id);
CREATE INDEX IF NOT EXISTS idx_player_matches_hero ON player_matches(hero_id);

-- Item purchase events (flattened purchase_log)
CREATE TABLE IF NOT EXISTS item_purchase_event (
  match_id BIGINT NOT NULL,
  account_id BIGINT NOT NULL,
  hero_id INT NOT NULL,
  time_s INT NOT NULL,
  item_key TEXT NOT NULL,
  is_radiant BOOLEAN NOT NULL,
  patch INT,
  PRIMARY KEY (match_id, account_id, item_key, time_s),
  FOREIGN KEY (match_id, account_id) REFERENCES player_matches(match_id, account_id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_ipe_patch_time ON item_purchase_event(patch, time_s);
CREATE INDEX IF NOT EXISTS idx_ipe_hero_time ON item_purchase_event(hero_id, time_s);

-- Materialized view of team 5-hero picks
CREATE MATERIALIZED VIEW IF NOT EXISTS pro_team_picks_mv AS
SELECT
  m.match_id,
  m.start_time,
  m.patch,
  COALESCE(tm.team_id, CASE WHEN pb.team = 0 THEN m.radiant_team_id ELSE m.dire_team_id END)::bigint AS team_id,
  (pb.team = 0) AS is_radiant,
  array_agg(pb.hero_id ORDER BY pb.ord) AS pick_heroes
FROM matches m
JOIN picks_bans pb ON pb.match_id = m.match_id AND pb.is_pick = true
LEFT JOIN team_match tm
  ON tm.match_id = m.match_id AND ((tm.radiant = true AND pb.team = 0) OR (tm.radiant = false AND pb.team = 1))
GROUP BY
  m.match_id,
  m.start_time,
  m.patch,
  COALESCE(tm.team_id, CASE WHEN pb.team = 0 THEN m.radiant_team_id ELSE m.dire_team_id END),
  pb.team;

CREATE UNIQUE INDEX IF NOT EXISTS idx_pro_team_picks_pk ON pro_team_picks_mv(match_id, team_id);
CREATE INDEX IF NOT EXISTS idx_pro_team_picks_time ON pro_team_picks_mv(start_time);
CREATE INDEX IF NOT EXISTS idx_pro_team_picks_patch ON pro_team_picks_mv(patch);

-- Aggregates: hero trends and pairs
CREATE TABLE IF NOT EXISTS pro_hero_trends (
  bucket_type TEXT NOT NULL,
  bucket_value TEXT NOT NULL,
  hero_id INT NOT NULL,
  matches BIGINT NOT NULL,
  picks BIGINT NOT NULL,
  pick_rate DOUBLE PRECISION NOT NULL,
  win_rate DOUBLE PRECISION,
  delta_vs_prev DOUBLE PRECISION,
  computed_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  PRIMARY KEY (bucket_type, bucket_value, hero_id)
);

CREATE INDEX IF NOT EXISTS idx_pro_hero_trends_bucket ON pro_hero_trends(bucket_type, bucket_value);

CREATE TABLE IF NOT EXISTS pro_hero_pair_stats (
  bucket_type TEXT NOT NULL,
  bucket_value TEXT NOT NULL,
  hero_id_a INT NOT NULL,
  hero_id_b INT NOT NULL,
  games_together BIGINT NOT NULL,
  wins_together BIGINT,
  support DOUBLE PRECISION,
  confidence DOUBLE PRECISION,
  lift DOUBLE PRECISION,
  computed_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  PRIMARY KEY (bucket_type, bucket_value, hero_id_a, hero_id_b),
  CHECK (hero_id_a < hero_id_b)
);

CREATE INDEX IF NOT EXISTS idx_pro_hero_pair_bucket ON pro_hero_pair_stats(bucket_type, bucket_value);


