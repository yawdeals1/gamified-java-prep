-- Backfill the original single-user data to the owner, then enforce tenant keys.
DO $$
DECLARE
    owner_id CONSTANT VARCHAR(100) := '769223c3-f7be-464e-833b-28badc5a8c6f';
BEGIN
    ALTER TABLE module_progress ADD COLUMN IF NOT EXISTS auth_user_id VARCHAR(100);
    ALTER TABLE step_progress ADD COLUMN IF NOT EXISTS auth_user_id VARCHAR(100);
    ALTER TABLE quiz_attempt ADD COLUMN IF NOT EXISTS auth_user_id VARCHAR(100);
    ALTER TABLE challenge_submission ADD COLUMN IF NOT EXISTS auth_user_id VARCHAR(100);
    ALTER TABLE achievement ADD COLUMN IF NOT EXISTS auth_user_id VARCHAR(100);
    ALTER TABLE xp_log ADD COLUMN IF NOT EXISTS auth_user_id VARCHAR(100);
    ALTER TABLE app_state ADD COLUMN IF NOT EXISTS auth_user_id VARCHAR(100);

    UPDATE module_progress SET auth_user_id = owner_id WHERE auth_user_id IS NULL;
    UPDATE step_progress SET auth_user_id = owner_id WHERE auth_user_id IS NULL;
    UPDATE quiz_attempt SET auth_user_id = owner_id WHERE auth_user_id IS NULL;
    UPDATE challenge_submission SET auth_user_id = owner_id WHERE auth_user_id IS NULL;
    UPDATE achievement SET auth_user_id = owner_id WHERE auth_user_id IS NULL;
    UPDATE xp_log SET auth_user_id = owner_id WHERE auth_user_id IS NULL;
    UPDATE app_state SET auth_user_id = owner_id WHERE auth_user_id IS NULL;
END $$;

ALTER TABLE module_progress ALTER COLUMN auth_user_id SET NOT NULL;
ALTER TABLE step_progress ALTER COLUMN auth_user_id SET NOT NULL;
ALTER TABLE quiz_attempt ALTER COLUMN auth_user_id SET NOT NULL;
ALTER TABLE challenge_submission ALTER COLUMN auth_user_id SET NOT NULL;
ALTER TABLE achievement ALTER COLUMN auth_user_id SET NOT NULL;
ALTER TABLE xp_log ALTER COLUMN auth_user_id SET NOT NULL;
ALTER TABLE app_state ALTER COLUMN auth_user_id SET NOT NULL;

ALTER TABLE module_progress DROP CONSTRAINT IF EXISTS module_progress_module_id_key;
ALTER TABLE achievement DROP CONSTRAINT IF EXISTS achievement_name_key;
ALTER TABLE app_state ALTER COLUMN id DROP DEFAULT;
CREATE SEQUENCE IF NOT EXISTS app_state_id_seq OWNED BY app_state.id;
SELECT setval('app_state_id_seq', COALESCE((SELECT MAX(id) FROM app_state), 0) + 1, false);
ALTER TABLE app_state ALTER COLUMN id SET DEFAULT nextval('app_state_id_seq');

CREATE UNIQUE INDEX IF NOT EXISTS module_progress_user_module_uq
    ON module_progress(auth_user_id, module_id);
CREATE UNIQUE INDEX IF NOT EXISTS step_progress_user_step_uq
    ON step_progress(auth_user_id, step_id);
CREATE UNIQUE INDEX IF NOT EXISTS achievement_user_name_uq
    ON achievement(auth_user_id, name);
CREATE UNIQUE INDEX IF NOT EXISTS app_state_user_uq
    ON app_state(auth_user_id);
CREATE INDEX IF NOT EXISTS quiz_attempt_user_module_idx
    ON quiz_attempt(auth_user_id, module_id);
CREATE INDEX IF NOT EXISTS challenge_submission_user_module_idx
    ON challenge_submission(auth_user_id, module_id);
CREATE INDEX IF NOT EXISTS xp_log_user_created_idx
    ON xp_log(auth_user_id, created_at DESC);
