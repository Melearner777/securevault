-- Delete all users from SecureVault database
-- WARNING: This will permanently delete all user accounts and passwords!

USE securevault_db;

-- Clear all users
DELETE FROM users;

-- Verify deletion
SELECT COUNT(*) as total_users_remaining FROM users;

-- Show remaining records (should be empty)
SELECT * FROM users;

