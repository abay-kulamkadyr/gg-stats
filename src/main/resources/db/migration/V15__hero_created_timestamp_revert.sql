-- Revert step 3: Remove NOT NULL and DEFAULT constraints
-- This must be done first to avoid conflicts
ALTER TABLE hero
  ALTER COLUMN created_at DROP NOT NULL,
  ALTER COLUMN created_at DROP DEFAULT;

-- Revert step 2: Set all of the existing values to null
UPDATE hero
SET created_at = NULL;

-- Revert step 1: Change the column type back
ALTER TABLE hero
  ALTER COLUMN created_at TYPE TIMESTAMP WITH TIME ZONE;