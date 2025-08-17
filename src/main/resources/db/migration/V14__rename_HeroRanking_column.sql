ALTER TABLE player_ratings
RENAME COLUMN recorded_at TO updated_at;

CREATE TRIGGER update_player_ratings_updated_at BEFORE UPDATE ON player_ratings
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
