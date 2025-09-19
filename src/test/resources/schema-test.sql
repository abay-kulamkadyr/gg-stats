-- Create the hero table with H2-compatible data types
CREATE TABLE IF NOT EXISTS hero (
    id INT NOT NULL,
    created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP(6) WITH TIME ZONE,
    attack_type VARCHAR(255) NOT NULL,
    localized_name VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL UNIQUE,
    primary_attr VARCHAR(255) NOT NULL,
    roles VARCHAR(255) NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS hero_ranking (
    account_id BIGINT NOT NULL,
    hero_id INTEGER NOT NULL,
    score DOUBLE PRECISION,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,

    CONSTRAINT pk_hero_ranking PRIMARY KEY (account_id, hero_id)
);

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
  created_at TIMESTAMP WITH TIME ZONE DEFAULT now(),
  updated_at TIMESTAMP WITH TIME ZONE DEFAULT now()
);

CREATE TABLE IF NOT EXISTS notable_player (
    account_id BIGINT,
    country_code VARCHAR(10),
    fantasy_role INT,
    team_id BIGINT,
    name VARCHAR(255),
    is_locked BOOLEAN DEFAULT FALSE,
    is_pro BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS player (
    account_id BIGINT PRIMARY KEY,
    steam_id VARCHAR(50),
    avatar TEXT,
    avatarmedium TEXT,
    avatarfull TEXT,
    profileurl TEXT,
    personname VARCHAR(255),
    last_login TIMESTAMP WITH TIME ZONE,
    full_history_time TIMESTAMP WITH TIME ZONE,
    cheese INT DEFAULT 0,
    fh_unavailable BOOLEAN DEFAULT FALSE,
    loccountrycode VARCHAR(10),
    last_match_time TIMESTAMP WITH TIME ZONE,
    plus BOOLEAN DEFAULT FALSE,
    leaderboard_rank INT,
    rank_tier INT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE IF NOT EXISTS api_rate_limit (
    id SERIAL PRIMARY KEY,
    endpoint VARCHAR(255) NOT NULL,
    requests_count INT DEFAULT 0,
    window_start TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    daily_requests INT DEFAULT 0,
    daily_window_start DATE DEFAULT CURRENT_DATE
);


CREATE TABLE IF NOT EXISTS team (
    team_id BIGINT PRIMARY KEY,
    rating INT,
    wins INT DEFAULT 0,
    losses INT DEFAULT 0,
    last_match_time BIGINT, -- Unix timestamp from API
    name VARCHAR(255),
    tag VARCHAR(50),
    logo_url TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
