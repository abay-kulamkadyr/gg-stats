CREATE TABLE hero (
    id BIGINT PRIMARY KEY,
    name TEXT NOT NULL,
    localized_name TEXT NOT NULL,
    primary_attr TEXT,
    attack_type TEXT,
    roles TEXT[]
);

CREATE TABLE professional_match (
    match_id BIGINT PRIMARY KEY,
    start_time TIMESTAMP,
    duration INT,
    radiant_win BOOLEAN,
    radiant_team TEXT,
    dire_team TEXT
);

CREATE TABLE player_synergy (
    account_id BIGINT NOT NULL,
    teammate_id BIGINT NOT NULL,
    games INT,
    wins INT,
    PRIMARY KEY (account_id, teammate_id)
);
