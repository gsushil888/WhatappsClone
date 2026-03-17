-- Fix Duplicate Conversation Participants
-- This script identifies and removes duplicate participant records

-- 1. Find duplicate participants (same user in same conversation)
SELECT 
    conversation_id, 
    user_id, 
    COUNT(*) as duplicate_count
FROM conversation_participants
WHERE status = 'ACTIVE'
GROUP BY conversation_id, user_id
HAVING COUNT(*) > 1;

-- 2. Keep only the most recent participant record and delete older duplicates
-- This keeps the record with the latest joined_at timestamp
DELETE cp1 FROM conversation_participants cp1
INNER JOIN conversation_participants cp2 
WHERE cp1.conversation_id = cp2.conversation_id
  AND cp1.user_id = cp2.user_id
  AND cp1.status = 'ACTIVE'
  AND cp2.status = 'ACTIVE'
  AND cp1.joined_at < cp2.joined_at;

-- 3. Verify no duplicates remain
SELECT 
    conversation_id, 
    user_id, 
    COUNT(*) as count
FROM conversation_participants
WHERE status = 'ACTIVE'
GROUP BY conversation_id, user_id
HAVING COUNT(*) > 1;

-- 4. Add unique constraint to prevent future duplicates (optional but recommended)
-- ALTER TABLE conversation_participants 
-- ADD UNIQUE INDEX idx_unique_active_participant (conversation_id, user_id, status);
