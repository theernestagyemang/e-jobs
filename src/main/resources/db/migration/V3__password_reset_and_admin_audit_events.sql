-- Two changes that must land together with the new AuditEventType values.
--
-- 1. audit_logs_event_type_check (V1) enumerates the five original event types. Without
--    widening it, every ADMIN_CREATED / PASSWORD_RESET_* insert fails at runtime with a
--    check-constraint violation - the enum compiles fine, so this only shows up in
--    production behaviour.
ALTER TABLE audit_logs
    DROP CONSTRAINT audit_logs_event_type_check;

ALTER TABLE audit_logs
    ADD CONSTRAINT audit_logs_event_type_check CHECK (event_type IN (
        'LOGIN_SUCCESS',
        'LOGIN_FAILURE',
        'REGISTER',
        'USER_DEACTIVATED',
        'USER_REACTIVATED',
        'ADMIN_CREATED',
        'PASSWORD_RESET_REQUESTED',
        'PASSWORD_RESET_COMPLETED'
    ));

-- 2. Storage for the forgot-password flow.
CREATE TABLE password_reset_tokens (
    id         uuid                        NOT NULL,
    user_id    uuid                        NOT NULL,
    token      character varying(255)      NOT NULL,
    expires_at timestamp(6) with time zone NOT NULL,
    used       boolean DEFAULT false       NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    CONSTRAINT password_reset_tokens_pkey PRIMARY KEY (id),
    CONSTRAINT fk_password_reset_tokens_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT uk_password_reset_tokens_token UNIQUE (token)
);

-- Redemption looks a token up by value on every reset attempt.
CREATE INDEX idx_password_reset_tokens_user_id ON password_reset_tokens USING btree (user_id);
