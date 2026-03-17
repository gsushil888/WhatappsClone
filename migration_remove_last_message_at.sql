-- Migration: Remove last_message_at column from conversations table
-- Reason: Redundant field causing conflicts with lastMessage.timestamp

-- Drop the index first
DROP INDEX IF EXISTS idx_conversation_updated ON conversations;

-- Drop the column
ALTER TABLE conversations DROP COLUMN IF EXISTS last_message_at;
