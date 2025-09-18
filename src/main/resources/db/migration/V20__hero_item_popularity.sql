-- Popular items by hero and time bucket (start/early/mid/late)
-- Buckets:
--  start_game: 0 <= t < 600s
--  early_game: 600 <= t < 1200s
--  mid_game:   1200 <= t < 1800s
--  late_game:  t >= 1800s

CREATE MATERIALIZED VIEW IF NOT EXISTS pro_hero_item_popularity_mv AS
WITH bucketed AS (
  SELECT
    hero_id,
    CASE
      WHEN time_s >= 0   AND time_s <  600  THEN 'start_game'
      WHEN time_s >= 600 AND time_s <  1200 THEN 'early_game'
      WHEN time_s >= 1200 AND time_s < 1800 THEN 'mid_game'
      ELSE 'late_game'
    END AS time_bucket,
    item_key,
    COUNT(*) AS purchases
  FROM item_purchase_event
  GROUP BY hero_id, time_bucket, item_key
)
SELECT * FROM bucketed;

-- Unique index required for CONCURRENT refreshes
CREATE UNIQUE INDEX IF NOT EXISTS idx_pro_hip_mv_pk
  ON pro_hero_item_popularity_mv(hero_id, time_bucket, item_key);

-- Helper indexes
CREATE INDEX IF NOT EXISTS idx_pro_hip_mv_hero_bucket ON pro_hero_item_popularity_mv(hero_id, time_bucket);
CREATE INDEX IF NOT EXISTS idx_pro_hip_mv_purchases ON pro_hero_item_popularity_mv(hero_id, time_bucket, purchases DESC);










