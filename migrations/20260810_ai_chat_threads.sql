-- Thread the former single AI history into private, resumable user chats.
CREATE TABLE IF NOT EXISTS ai_chat (
    id SERIAL PRIMARY KEY,
    auth_user_id VARCHAR(100) NOT NULL,
    title VARCHAR(120) NOT NULL DEFAULT 'New chat',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS ai_chat_user_updated_idx
    ON ai_chat(auth_user_id, updated_at DESC);

ALTER TABLE ai_conversation
    ADD COLUMN IF NOT EXISTS chat_id INT REFERENCES ai_chat(id) ON DELETE CASCADE;

ALTER TABLE ai_conversation
    ADD COLUMN IF NOT EXISTS auth_user_id VARCHAR(100);

CREATE INDEX IF NOT EXISTS ai_conversation_chat_created_idx
    ON ai_conversation(chat_id, created_at);

-- Preserve the pre-thread history as the owner's "Previous conversation".
DO $$
DECLARE
    owner_id CONSTANT VARCHAR(100) := '769223c3-f7be-464e-833b-28badc5a8c6f';
    legacy_chat_id INT;
BEGIN
    IF EXISTS (SELECT 1 FROM ai_conversation WHERE chat_id IS NULL AND auth_user_id IS NULL) THEN
        SELECT id INTO legacy_chat_id
        FROM ai_chat
        WHERE auth_user_id = owner_id AND title = 'Previous conversation'
        ORDER BY id
        LIMIT 1;

        IF legacy_chat_id IS NULL THEN
            INSERT INTO ai_chat (auth_user_id, title, created_at, updated_at)
            SELECT owner_id,
                   'Previous conversation',
                   COALESCE(MIN(created_at), CURRENT_TIMESTAMP),
                   COALESCE(MAX(created_at), CURRENT_TIMESTAMP)
            FROM ai_conversation
            WHERE chat_id IS NULL AND auth_user_id IS NULL
            RETURNING id INTO legacy_chat_id;
        END IF;

        UPDATE ai_conversation
        SET chat_id = legacy_chat_id,
            auth_user_id = owner_id
        WHERE chat_id IS NULL AND auth_user_id IS NULL;
    END IF;
END $$;

UPDATE ai_conversation
SET auth_user_id = '769223c3-f7be-464e-833b-28badc5a8c6f'
WHERE auth_user_id IS NULL;

ALTER TABLE ai_conversation
    ALTER COLUMN auth_user_id SET NOT NULL;
