DROP INDEX IF EXISTS xp_log_user_created_idx;
DROP INDEX IF EXISTS challenge_submission_user_module_idx;
DROP INDEX IF EXISTS quiz_attempt_user_module_idx;
DROP INDEX IF EXISTS app_state_user_uq;
DROP INDEX IF EXISTS achievement_user_name_uq;
DROP INDEX IF EXISTS step_progress_user_step_uq;
DROP INDEX IF EXISTS module_progress_user_module_uq;

ALTER TABLE app_state DROP COLUMN IF EXISTS auth_user_id;
ALTER TABLE xp_log DROP COLUMN IF EXISTS auth_user_id;
ALTER TABLE achievement DROP COLUMN IF EXISTS auth_user_id;
ALTER TABLE challenge_submission DROP COLUMN IF EXISTS auth_user_id;
ALTER TABLE quiz_attempt DROP COLUMN IF EXISTS auth_user_id;
ALTER TABLE step_progress DROP COLUMN IF EXISTS auth_user_id;
ALTER TABLE module_progress DROP COLUMN IF EXISTS auth_user_id;
