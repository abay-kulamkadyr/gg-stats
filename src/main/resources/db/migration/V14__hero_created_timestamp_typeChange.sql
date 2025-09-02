-- 1) Convert type (if needed), interpreting existing values as UTC
ALTER TABLE hero
  ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at AT TIME ZONE 'UTC';

-- 2) Backfill NULLs before NOT NULL
UPDATE hero
SET created_at = COALESCE(updated_at, CURRENT_TIMESTAMP)
WHERE created_at IS NULL;

-- 3) Add default and NOT NULL
ALTER TABLE hero
  ALTER COLUMN created_at SET DEFAULT CURRENT_TIMESTAMP,
  ALTER COLUMN created_at SET NOT NULL;