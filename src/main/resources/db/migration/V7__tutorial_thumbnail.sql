-- Add thumbnailUrl column to tutorials for YouTube thumbnail support.
ALTER TABLE tutorials ADD COLUMN IF NOT EXISTS thumbnail_url VARCHAR(1000);
