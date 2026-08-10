ALTER TABLE ai_conversation DROP COLUMN IF EXISTS chat_id;
ALTER TABLE ai_conversation DROP COLUMN IF EXISTS auth_user_id;
DROP TABLE IF EXISTS ai_chat;
