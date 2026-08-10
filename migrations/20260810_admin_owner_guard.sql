ALTER TABLE app_user
    ADD CONSTRAINT app_user_admin_owner_check CHECK (
        role <> 'ADMIN' OR auth_user_id = '769223c3-f7be-464e-833b-28badc5a8c6f'
    );
