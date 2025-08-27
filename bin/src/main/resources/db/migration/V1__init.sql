-- Drop tables in correct order to handle dependencies
DROP TABLE IF EXISTS player_ratings CASCADE;
DROP TABLE IF EXISTS leaderboard_rank CASCADE;
DROP TABLE IF EXISTS rank_tier CASCADE;
DROP TABLE IF EXISTS pro_player CASCADE;
DROP TABLE IF EXISTS team CASCADE;
DROP TABLE IF EXISTS player CASCADE;
DROP TABLE IF EXISTS hero CASCADE;
DROP TABLE IF EXISTS api_rate_limit CASCADE;

-- Heroes table
CREATE TABLE IF NOT EXISTS hero (
    id INT PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    localized_name VARCHAR(100) NOT NULL,
    primary_attr VARCHAR(20) CHECK (primary_attr IN ('str', 'agi', 'int', 'all')),
    attack_type VARCHAR(20) CHECK (attack_type IN ('Melee', 'Ranged')),
    roles TEXT[], -- Array of roles like ['Carry', 'Support']
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Teams table (moved before players due to pro_player reference)
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

-- Players table
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
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Pro players table
CREATE TABLE IF NOT EXISTS pro_player (
    account_id BIGINT PRIMARY KEY REFERENCES player(account_id),
    country_code VARCHAR(10),
    fantasy_role INT,
    team_id BIGINT REFERENCES team(team_id),
    name VARCHAR(255),
    is_locked BOOLEAN DEFAULT FALSE,
    is_pro BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Current rank tier for players
CREATE TABLE IF NOT EXISTS rank_tier (
    account_id BIGINT PRIMARY KEY REFERENCES player(account_id),
    rating INT,
    rank_tier INT, -- 0-80+ rank tier from API
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Leaderboard rankings
CREATE TABLE IF NOT EXISTS leaderboard_rank (
    account_id BIGINT PRIMARY KEY REFERENCES player(account_id),
    rank_position INT,
    rating INT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Historical player ratings
CREATE TABLE IF NOT EXISTS player_ratings (
    id BIGSERIAL PRIMARY KEY,
    account_id BIGINT REFERENCES player(account_id),
    solo_competitive_rank INT,
    competitive_rank INT,
    recorded_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- API rate limiting tracking
CREATE TABLE IF NOT EXISTS api_rate_limit (
    id SERIAL PRIMARY KEY,
    endpoint VARCHAR(255) NOT NULL,
    requests_count INT DEFAULT 0,
    window_start TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    daily_requests INT DEFAULT 0,
    daily_window_start DATE DEFAULT CURRENT_DATE
);

-- Indexes for better performance
CREATE INDEX IF NOT EXISTS idx_player_account_id ON player(account_id);
CREATE INDEX IF NOT EXISTS idx_pro_player_team_id ON pro_player(team_id);
CREATE INDEX IF NOT EXISTS idx_player_ratings_account_id ON player_ratings(account_id);
CREATE INDEX IF NOT EXISTS idx_player_ratings_recorded_at ON player_ratings(recorded_at);
CREATE INDEX IF NOT EXISTS idx_hero_name ON hero(name);
CREATE INDEX IF NOT EXISTS idx_team_name ON team(name);

-- Triggers for updated_at timestamps
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_hero_updated_at BEFORE UPDATE ON hero
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_player_updated_at BEFORE UPDATE ON player
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_team_updated_at BEFORE UPDATE ON team
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_pro_player_updated_at BEFORE UPDATE ON pro_player
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_rank_tier_updated_at BEFORE UPDATE ON rank_tier
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_leaderboard_rank_updated_at BEFORE UPDATE ON leaderboard_rank
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();