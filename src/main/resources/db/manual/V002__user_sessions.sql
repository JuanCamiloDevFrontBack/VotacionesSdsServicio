-- Migración manual: tabla user_sessions para manejo de sesiones server-side.
-- Ejecutar: psql -h localhost -p 5480 -U admin -d votaciones_sds -f src/main/resources/db/manual/V002__user_sessions.sql

BEGIN;

CREATE TABLE IF NOT EXISTS user_sessions (
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

CREATE INDEX IF NOT EXISTS idx_user_sessions_user_id    ON user_sessions(user_id);
CREATE INDEX IF NOT EXISTS idx_user_sessions_expires_at ON user_sessions(expires_at);
CREATE INDEX IF NOT EXISTS idx_user_sessions_active     ON user_sessions(user_id, revoked) WHERE revoked = false;

COMMIT;
