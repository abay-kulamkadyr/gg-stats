ALTER TABLE pro_player DROP CONSTRAINT IF EXISTS pro_player_account_id_fkey;
ALTER TABLE pro_player DROP CONSTRAINT IF EXISTS pro_player_pkey;
ALTER TABLE pro_player ADD CONSTRAINT pro_player_pkey PRIMARY KEY (account_id);