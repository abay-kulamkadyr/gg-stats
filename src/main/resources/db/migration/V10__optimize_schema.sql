ALTER TABLE notable_player RENAME CONSTRAINT pro_player_pkey TO notable_player_pkey;

-- Indexes for performance
CREATE INDEX idx_hero_ranking_hero_id ON hero_ranking(hero_id);
CREATE INDEX idx_hero_ranking_score ON hero_ranking(score DESC); -- for top rankings
CREATE INDEX idx_notable_player_team_id ON notable_player(team_id);
CREATE INDEX idx_api_rate_limit_endpoint ON api_rate_limit(endpoint);


-- Some reasonable constraints:
ALTER TABLE hero_ranking ADD CONSTRAINT hero_ranking_score_positive CHECK (score >= 0);
ALTER TABLE team ADD CONSTRAINT team_wins_positive CHECK (wins >= 0);
ALTER TABLE team ADD CONSTRAINT team_losses_positive CHECK (losses >= 0);

-- Remove unused tables
DROP TABLE IF EXISTS player_ratings;
DROP TABLE IF EXISTS rank_tier;
