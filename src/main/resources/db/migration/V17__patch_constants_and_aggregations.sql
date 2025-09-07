-- Patch constants (from OpenDota /constants/patch)
CREATE TABLE IF NOT EXISTS patch_constants (
  id INT PRIMARY KEY,
  name TEXT NOT NULL,
  start_time TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_patch_constants_start ON patch_constants(start_time);

-- Helper view: patch id to human name
CREATE OR REPLACE VIEW patch_lookup AS
SELECT id AS patch_id, name AS patch_name, start_time FROM patch_constants;




