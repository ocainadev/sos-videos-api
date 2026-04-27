-- Remove the first_login_pending_password_change column entirely.
-- The feature has been disabled and all references have been removed from the codebase.
ALTER TABLE users DROP COLUMN IF EXISTS first_login_pending_password_change;
