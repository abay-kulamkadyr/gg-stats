-- Add delta columns for pair trends by week
ALTER TABLE pro_hero_pair_stats
  ADD COLUMN IF NOT EXISTS delta_support DOUBLE PRECISION,
  ADD COLUMN IF NOT EXISTS delta_lift DOUBLE PRECISION;



