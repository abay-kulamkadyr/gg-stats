CREATE TABLE IF NOT EXISTS hero_ranking (
  account_id BIGINT,
  hero_id INT REFERENCES hero (id),
  score DOUBLE PRECISION,
  created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TRIGGER update_hero_ranking_updated_at BEFORE UPDATE ON hero_ranking
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
