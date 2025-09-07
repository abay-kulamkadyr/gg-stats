-- Fix REFRESH CONCURRENTLY failures caused by duplicate (match_id, team_id)
-- When team_id is 0/unknown for both sides, both rows share the same team_id.
-- Use (match_id, is_radiant) as the MV's unique key instead.

DROP INDEX IF EXISTS idx_pro_team_picks_pk;
CREATE UNIQUE INDEX idx_pro_team_picks_pk ON pro_team_picks_mv(match_id, is_radiant);



