-- Migration: Add conversation filter fields
-- Run this SQL script on your database before starting the application

-- Add filter columns to conversation_participants table
ALTER TABLE conversation_participants 
ADD COLUMN IF NOT EXISTS is_favorite BOOLEAN DEFAULT FALSE NOT NULL,
ADD COLUMN IF NOT EXISTS is_archived BOOLEAN DEFAULT FALSE NOT NULL,
ADD COLUMN IF NOT EXISTS is_pinned BOOLEAN DEFAULT FALSE NOT NULL,
ADD COLUMN IF NOT EXISTS is_muted BOOLEAN DEFAULT FALSE NOT NULL,
ADD COLUMN IF NOT EXISTS mute_until TIMESTAMP NULL;

-- Update existing rows to have default values
UPDATE conversation_participants 
SET is_favorite = FALSE WHERE is_favorite IS NULL;

UPDATE conversation_participants 
SET is_archived = FALSE WHERE is_archived IS NULL;

UPDATE conversation_participants 
SET is_pinned = FALSE WHERE is_pinned IS NULL;

UPDATE conversation_participants 
SET is_muted = FALSE WHERE is_muted IS NULL;

-- Add indexes for better query performance
CREATE INDEX IF NOT EXISTS idx_participant_favorite ON conversation_participants(user_id, is_favorite);
CREATE INDEX IF NOT EXISTS idx_participant_archived ON conversation_participants(user_id, is_archived);
CREATE INDEX IF NOT EXISTS idx_participant_pinned ON conversation_participants(user_id, is_pinned);
CREATE INDEX IF NOT EXISTS idx_participant_muted ON conversation_participants(user_id, is_muted);

-- Verify the changes
SELECT 
    COLUMN_NAME, 
    DATA_TYPE, 
    IS_NULLABLE, 
    COLUMN_DEFAULT 
FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_NAME = 'conversation_participants' 
AND COLUMN_NAME IN ('is_favorite', 'is_archived', 'is_pinned', 'is_muted', 'mute_until')
ORDER BY COLUMN_NAME;
