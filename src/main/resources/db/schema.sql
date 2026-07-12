-- ------------------------------------------------------------
-- 1. ROLES (RBAC simple)
-- ------------------------------------------------------------
CREATE TABLE roles (
    id   SMALLSERIAL PRIMARY KEY,
    name VARCHAR(50) UNIQUE NOT NULL   -- ROLE_USER, ROLE_ADMIN...
);

INSERT INTO roles (name) VALUES ('ROLE_USER'), ('ROLE_ADMIN');

-- ------------------------------------------------------------
-- 2. USUARIOS
-- ------------------------------------------------------------
CREATE TABLE users (
    id                     UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email                  VARCHAR(255) UNIQUE NOT NULL,
    username               VARCHAR(50)  UNIQUE NOT NULL,
    password_hash          VARCHAR(255) NOT NULL,
    enabled                BOOLEAN NOT NULL DEFAULT TRUE,
    email_verified         BOOLEAN NOT NULL DEFAULT FALSE,
    account_locked         BOOLEAN NOT NULL DEFAULT FALSE,
    failed_login_attempts  SMALLINT NOT NULL DEFAULT 0,
    locked_until           TIMESTAMPTZ,
    password_changed_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_at             TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at             TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_users_email ON users(email);

-- ------------------------------------------------------------
-- 3. RELACIÓN USUARIO-ROL (N:M)
-- ------------------------------------------------------------
CREATE TABLE user_roles (
    user_id UUID     NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_id SMALLINT NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

-- ------------------------------------------------------------
-- 4. REFRESH TOKENS
-- ------------------------------------------------------------
CREATE TABLE refresh_tokens (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id      UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash   VARCHAR(255) NOT NULL UNIQUE,
    family_id    UUID NOT NULL,
    issued_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    expires_at   TIMESTAMPTZ NOT NULL,
    revoked      BOOLEAN NOT NULL DEFAULT FALSE,
    revoked_at   TIMESTAMPTZ,
    replaced_by  UUID REFERENCES refresh_tokens(id),
    user_agent   VARCHAR(255),
    ip_address   VARCHAR(45)
);

CREATE INDEX idx_refresh_tokens_user_id    ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_family_id  ON refresh_tokens(family_id);
CREATE INDEX idx_refresh_tokens_expires_at ON refresh_tokens(expires_at);

-- ------------------------------------------------------------
-- 5. AUDITORÍA DE LOGIN
-- ------------------------------------------------------------
CREATE TABLE login_audit (
    id              BIGSERIAL PRIMARY KEY,
    user_id         UUID REFERENCES users(id) ON DELETE SET NULL,
    email_attempted VARCHAR(255),
    success         BOOLEAN NOT NULL,
    ip_address      VARCHAR(45),
    user_agent      VARCHAR(255),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_login_audit_user_id    ON login_audit(user_id);
CREATE INDEX idx_login_audit_created_at ON login_audit(created_at);

-- ------------------------------------------------------------
-- 6. SESIONES DE USUARIO (server-side, vinculadas a refresh token family)
-- ------------------------------------------------------------
CREATE TABLE user_sessions (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id      UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    family_id    UUID NOT NULL UNIQUE,
    ip_address   VARCHAR(45),
    user_agent   VARCHAR(255),
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    last_seen_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    expires_at   TIMESTAMPTZ NOT NULL,
    revoked      BOOLEAN NOT NULL DEFAULT FALSE,
    revoked_at   TIMESTAMPTZ
);

CREATE INDEX idx_user_sessions_user_id    ON user_sessions(user_id);
CREATE INDEX idx_user_sessions_expires_at ON user_sessions(expires_at);
CREATE INDEX idx_user_sessions_active     ON user_sessions(user_id, revoked) WHERE revoked = false;
