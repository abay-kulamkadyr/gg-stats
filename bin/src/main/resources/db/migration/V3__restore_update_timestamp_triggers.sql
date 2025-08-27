-- Migration: Recreate update_*_updated_at triggers

CREATE TRIGGER update_hero_updated_at
  BEFORE UPDATE ON hero
  FOR EACH ROW
  EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_player_updated_at
  BEFORE UPDATE ON player
  FOR EACH ROW
  EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_team_updated_at
  BEFORE UPDATE ON team
  FOR EACH ROW
  EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_pro_player_updated_at
  BEFORE UPDATE ON pro_player
  FOR EACH ROW
  EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_rank_tier_updated_at
  BEFORE UPDATE ON rank_tier
  FOR EACH ROW
  EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_leaderboard_rank_updated_at
  BEFORE UPDATE ON leaderboard_rank
  FOR EACH ROW
  EXECUTE FUNCTION update_updated_at_column();
