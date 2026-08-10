-- Modules (the 9 levels)
CREATE TABLE IF NOT EXISTS course_module (
    id SERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    slug VARCHAR(255) NOT NULL UNIQUE,
    description VARCHAR(500),
    content_markdown TEXT,
    order_index INT NOT NULL,
    xp_reward INT DEFAULT 100,
    challenge_instructions TEXT,
    challenge_template_code TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Quiz questions per module
CREATE TABLE IF NOT EXISTS quiz_question (
    id SERIAL PRIMARY KEY,
    module_id INT REFERENCES course_module(id) ON DELETE CASCADE,
    question_text TEXT NOT NULL,
    options TEXT NOT NULL,
    correct_index INT NOT NULL,
    explanation TEXT,
    difficulty VARCHAR(20) DEFAULT 'easy',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Per-module progress for the single user
CREATE TABLE IF NOT EXISTS module_progress (
    id SERIAL PRIMARY KEY,
    module_id INT REFERENCES course_module(id) ON DELETE CASCADE,
    status VARCHAR(20) DEFAULT 'locked',
    quiz_score INT DEFAULT 0,
    quiz_attempts INT DEFAULT 0,
    challenge_passed BOOLEAN DEFAULT false,
    challenge_attempts INT DEFAULT 0,
    completed_at TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(module_id)
);

-- Track individual quiz answers
CREATE TABLE IF NOT EXISTS quiz_attempt (
    id SERIAL PRIMARY KEY,
    module_id INT REFERENCES course_module(id) ON DELETE CASCADE,
    question_id INT REFERENCES quiz_question(id) ON DELETE CASCADE,
    selected_index INT,
    correct BOOLEAN,
    attempted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Code challenge submissions
CREATE TABLE IF NOT EXISTS challenge_submission (
    id SERIAL PRIMARY KEY,
    module_id INT REFERENCES course_module(id) ON DELETE CASCADE,
    source_code TEXT NOT NULL,
    compile_output TEXT,
    compile_success BOOLEAN DEFAULT false,
    ai_feedback TEXT,
    ai_score INT,
    passed BOOLEAN DEFAULT false,
    submitted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Achievements / badges
CREATE TABLE IF NOT EXISTS achievement (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    description VARCHAR(500),
    icon VARCHAR(100),
    unlocked_at TIMESTAMP
);

-- XP transaction log
CREATE TABLE IF NOT EXISTS xp_log (
    id SERIAL PRIMARY KEY,
    action VARCHAR(100) NOT NULL,
    xp_gained INT NOT NULL,
    note VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- AI conversation history
CREATE TABLE IF NOT EXISTS ai_conversation (
    id SERIAL PRIMARY KEY,
    role VARCHAR(20) NOT NULL,
    message TEXT NOT NULL,
    module_id INT REFERENCES course_module(id) ON DELETE SET NULL,
    context_type VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- App state (single user: XP total, streak, etc.)
CREATE TABLE IF NOT EXISTS app_state (
    id INT PRIMARY KEY DEFAULT 1,
    total_xp INT DEFAULT 0,
    current_level INT DEFAULT 1,
    streak_count INT DEFAULT 0,
    last_active_date DATE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Insert initial app state row
INSERT INTO app_state (id, total_xp, current_level, streak_count)
VALUES (1, 0, 1, 0)
ON CONFLICT (id) DO NOTHING;

-- Application RBAC. Deploro Auth owns credentials; this table owns access/roles.
CREATE TABLE IF NOT EXISTS app_user (
    id SERIAL PRIMARY KEY,
    auth_user_id VARCHAR(100) UNIQUE,
    email VARCHAR(320) NOT NULL UNIQUE,
    display_name VARCHAR(255),
    role VARCHAR(20) NOT NULL DEFAULT 'MEMBER' CHECK (role IN ('ADMIN', 'MEMBER')),
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT app_user_admin_owner_check CHECK (
        role <> 'ADMIN' OR auth_user_id = '769223c3-f7be-464e-833b-28badc5a8c6f'
    )
);

CREATE TABLE IF NOT EXISTS member_invitation (
    id SERIAL PRIMARY KEY,
    email VARCHAR(320) NOT NULL,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    invited_by VARCHAR(320) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    accepted_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS member_invitation_email_status_idx ON member_invitation(email, status);

CREATE TABLE IF NOT EXISTS user_ai_settings (
    id SERIAL PRIMARY KEY,
    auth_user_id VARCHAR(100) NOT NULL UNIQUE,
    encrypted_api_key TEXT NOT NULL,
    key_last_four VARCHAR(4) NOT NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
