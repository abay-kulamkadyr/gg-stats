ALTER TABLE leaderboard_rank DROP CONSTRAINT IF EXISTS leaderboard_rank_pkey;
ALTER TABLE leaderboard_rank DROP CONSTRAINT IF EXISTS leaderboard_rank_account_id_fkey;
ALTER TABLE leaderboard_rank ADD CONSTRAINT leaderboard_rank_pkey PRIMARY KEY (account_id);