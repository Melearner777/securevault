-- SecureVault: remove legacy 2FA columns from the users table
-- Run this after taking a backup of `securevault_db`.
-- It is safe to run only if you are sure 2FA is already removed from the application code.

USE securevault_db;

ALTER TABLE users
    DROP COLUMN IF EXISTS pendingAccessLevel,
    DROP COLUMN IF EXISTS otpExpiry,
    DROP COLUMN IF EXISTS twoFactorOtp,
    DROP COLUMN IF EXISTS twoFactorEnabled,
    DROP COLUMN IF EXISTS email;

-- Optional verification:
-- SHOW COLUMNS FROM users;

