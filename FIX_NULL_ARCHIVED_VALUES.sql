-- Fix existing NULL values in conversation_participants table
-- Run this SQL script to update all NULL values to FALSE

UPDATE conversation_participants 
SET is_archived = FALSE 
WHERE is_archived IS NULL;

UPDATE conversation_participants 
SET is_favorite = FALSE 
WHERE is_favorite IS NULL;

UPDATE conversation_participants 
SET is_pinned = FALSE 
WHERE is_pinned IS NULL;

UPDATE conversation_participants 
SET is_muted = FALSE 
WHERE is_muted IS NULL;

-- Verify the update
SELECT 
    COUNT(*) as total_records,
    SUM(CASE WHEN is_archived IS NULL THEN 1 ELSE 0 END) as null_archived,
    SUM(CASE WHEN is_favorite IS NULL THEN 1 ELSE 0 END) as null_favorite,
    SUM(CASE WHEN is_pinned IS NULL THEN 1 ELSE 0 END) as null_pinned,
    SUM(CASE WHEN is_muted IS NULL THEN 1 ELSE 0 END) as null_muted
FROM conversation_participants;
