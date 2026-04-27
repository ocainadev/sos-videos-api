-- Remove the first-login password change requirement for all existing users.
-- The feature has been disabled; no user should be blocked from accessing the platform.
UPDATE users SET first_login_pending_password_change = false
WHERE first_login_pending_password_change = true;
