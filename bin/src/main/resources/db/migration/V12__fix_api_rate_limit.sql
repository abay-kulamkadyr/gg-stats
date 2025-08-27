-- Fix API rate limit table schema
-- Add unique constraint on endpoint
ALTER TABLE api_rate_limit ADD CONSTRAINT uk_api_rate_limit_endpoint UNIQUE (endpoint);

-- Add indexes for time-based queries
CREATE INDEX idx_api_rate_limit_window_start ON api_rate_limit(window_start);
CREATE INDEX idx_api_rate_limit_daily_window_start ON api_rate_limit(daily_window_start);

-- Add check constraints for data integrity
ALTER TABLE api_rate_limit ADD CONSTRAINT chk_api_rate_limit_requests_count_positive CHECK (requests_count >= 0);
ALTER TABLE api_rate_limit ADD CONSTRAINT chk_api_rate_limit_daily_requests_positive CHECK (daily_requests >= 0);

-- Update existing records to ensure data consistency
UPDATE api_rate_limit SET 
    requests_count = CASE WHEN requests_count < 0 THEN 0 ELSE requests_count END,
    daily_requests = CASE WHEN daily_requests < 0 THEN 0 ELSE daily_requests END
WHERE requests_count < 0 OR daily_requests < 0;
