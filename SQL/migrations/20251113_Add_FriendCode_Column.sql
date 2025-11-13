-- Migration: Add FriendCode column to Users table
-- Date: 2025-11-13
-- Description: Adds FriendCode column to support short, user-friendly friend codes

-- Add FriendCode column to Users table
ALTER TABLE Users
ADD FriendCode VARCHAR(10) NULL;

-- Create index for fast friend code lookups
CREATE INDEX IX_Users_FriendCode ON Users(FriendCode);

-- Backfill friend codes for existing users (run after deployment)
-- This can be done in batches or via application logic on next login
-- UPDATE Users
-- SET FriendCode = dbo.GenerateFriendCode(FirebaseUid)
-- WHERE FriendCode IS NULL;

PRINT 'Migration completed: FriendCode column added to Users table';
