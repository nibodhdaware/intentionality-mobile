-- Intentionality App - Supabase Database Setup
-- Run this script in your Supabase SQL Editor

-- Create the app_entries table
CREATE TABLE IF NOT EXISTS app_entries (
  id BIGSERIAL PRIMARY KEY,
  app_name TEXT NOT NULL,
  package_name TEXT NOT NULL,
  reason TEXT,
  rating INTEGER NOT NULL CHECK (rating >= 1 AND rating <= 5),
  timestamp TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  user_id TEXT NOT NULL,
  created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Add indexes for better query performance
CREATE INDEX IF NOT EXISTS idx_app_entries_user_id ON app_entries(user_id);
CREATE INDEX IF NOT EXISTS idx_app_entries_timestamp ON app_entries(timestamp DESC);
CREATE INDEX IF NOT EXISTS idx_app_entries_package_name ON app_entries(package_name);
CREATE INDEX IF NOT EXISTS idx_app_entries_rating ON app_entries(rating);

-- Enable Row Level Security
ALTER TABLE app_entries ENABLE ROW LEVEL SECURITY;

-- Drop existing policies if they exist (for clean setup)
DROP POLICY IF EXISTS "Users can insert their own entries" ON app_entries;
DROP POLICY IF EXISTS "Users can read their own entries" ON app_entries;
DROP POLICY IF EXISTS "Anonymous users can insert" ON app_entries;
DROP POLICY IF EXISTS "Anonymous users can read their entries" ON app_entries;

-- Policy to allow authenticated users to insert their own entries
CREATE POLICY "Users can insert their own entries" ON app_entries
  FOR INSERT 
  TO authenticated
  WITH CHECK (auth.uid()::text = user_id);

-- Policy to allow authenticated users to read their own entries
CREATE POLICY "Users can read their own entries" ON app_entries
  FOR SELECT 
  TO authenticated
  USING (auth.uid()::text = user_id);

-- Policy to allow anonymous users to insert entries
CREATE POLICY "Anonymous users can insert" ON app_entries
  FOR INSERT 
  TO anon
  WITH CHECK (user_id LIKE 'dev-user-%' OR user_id = 'anonymous');

-- Policy to allow anonymous users to read their own entries
CREATE POLICY "Anonymous users can read their entries" ON app_entries
  FOR SELECT 
  TO anon
  USING (user_id LIKE 'dev-user-%' OR user_id = 'anonymous');

-- Create a view for usage analytics (optional)
CREATE OR REPLACE VIEW user_app_analytics AS
SELECT 
  user_id,
  app_name,
  package_name,
  COUNT(*) as total_entries,
  AVG(rating)::numeric(3,2) as avg_intentionality,
  MIN(timestamp) as first_entry,
  MAX(timestamp) as last_entry,
  COUNT(CASE WHEN rating <= 2 THEN 1 END) as intentional_count,
  COUNT(CASE WHEN rating >= 4 THEN 1 END) as mindless_count
FROM app_entries
GROUP BY user_id, app_name, package_name;

-- Create a function to get daily summary (optional)
CREATE OR REPLACE FUNCTION get_daily_summary(target_user_id TEXT, target_date DATE DEFAULT CURRENT_DATE)
RETURNS TABLE (
  app_name TEXT,
  entry_count BIGINT,
  avg_rating NUMERIC,
  most_common_reason TEXT
) AS $$
BEGIN
  RETURN QUERY
  SELECT 
    ae.app_name,
    COUNT(*) as entry_count,
    AVG(ae.rating)::numeric(3,2) as avg_rating,
    MODE() WITHIN GROUP (ORDER BY ae.reason) as most_common_reason
  FROM app_entries ae
  WHERE ae.user_id = target_user_id
    AND DATE(ae.timestamp) = target_date
  GROUP BY ae.app_name
  ORDER BY entry_count DESC;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Grant permissions
GRANT SELECT ON user_app_analytics TO authenticated, anon;
GRANT EXECUTE ON FUNCTION get_daily_summary TO authenticated, anon;

-- Insert some test data (optional - remove in production)
-- INSERT INTO app_entries (app_name, package_name, reason, rating, user_id)
-- VALUES 
--   ('Instagram', 'com.instagram.android', 'Checking notifications', 3, 'dev-user-test'),
--   ('Twitter', 'com.twitter.android', 'Reading news', 2, 'dev-user-test'),
--   ('Gmail', 'com.google.android.gm', 'Responding to work email', 1, 'dev-user-test');

-- Verify the setup
SELECT 'Setup completed successfully!' as status;
SELECT COUNT(*) as total_entries FROM app_entries;

