CREATE TABLE IF NOT EXISTS app_user (
    id SERIAL PRIMARY KEY,
    auth_user_id VARCHAR(100) UNIQUE,
    email VARCHAR(320) NOT NULL UNIQUE,
    display_name VARCHAR(255),
    role VARCHAR(20) NOT NULL DEFAULT 'MEMBER' CHECK (role IN ('ADMIN', 'MEMBER')),
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
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

INSERT INTO app_user (auth_user_id, email, display_name, role, active)
VALUES ('769223c3-f7be-464e-833b-28badc5a8c6f', 'calebmensah1502@gmail.com', 'Caleb Yaw Mensah', 'ADMIN', true)
ON CONFLICT (email) DO UPDATE SET
    auth_user_id = EXCLUDED.auth_user_id,
    display_name = EXCLUDED.display_name,
    role = 'ADMIN',
    active = true,
    updated_at = CURRENT_TIMESTAMP;
