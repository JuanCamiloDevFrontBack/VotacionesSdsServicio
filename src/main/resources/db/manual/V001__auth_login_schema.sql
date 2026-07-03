-- Migración manual: alineación de tablas de autenticación/login con entidades JPA.
-- Ejecutar: psql -h localhost -p 5480 -U admin -d votaciones_sds -f src/main/resources/db/manual/V001__auth_login_schema.sql

BEGIN;

-- =============================================================================
-- 1) users: sincronizar columnas duplicadas y defaults
-- =============================================================================

UPDATE public.users
SET
    account_non_locked = NOT account_locked,
    failed_attempt = failed_attempts
WHERE account_non_locked IS DISTINCT FROM NOT account_locked
   OR failed_attempt IS DISTINCT FROM failed_attempts;

ALTER TABLE public.users
    ALTER COLUMN account_non_locked SET DEFAULT true,
    ALTER COLUMN failed_attempt SET DEFAULT 0;

CREATE OR REPLACE FUNCTION public.sync_user_lock_columns()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    IF TG_OP = 'INSERT' THEN
        IF NEW.account_non_locked IS NULL THEN
            NEW.account_non_locked := NOT COALESCE(NEW.account_locked, false);
        END IF;
        IF NEW.failed_attempt IS NULL THEN
            NEW.failed_attempt := COALESCE(NEW.failed_attempts, 0);
        END IF;
    END IF;

    -- Mantener pares sincronizados (el código Java usa account_non_locked y failed_attempts)
    NEW.account_non_locked := NOT COALESCE(NEW.account_locked, false);
    NEW.account_locked := NOT NEW.account_non_locked;
    NEW.failed_attempt := COALESCE(NEW.failed_attempts, 0);
    NEW.failed_attempts := NEW.failed_attempt;

    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_sync_user_lock_columns ON public.users;

CREATE TRIGGER trg_sync_user_lock_columns
    BEFORE INSERT OR UPDATE ON public.users
    FOR EACH ROW
    EXECUTE FUNCTION public.sync_user_lock_columns();

-- =============================================================================
-- 2) users: convertir timestamps a timestamptz(6) (mapeo Instant en Hibernate 6)
-- =============================================================================

ALTER TABLE public.users
    ALTER COLUMN last_login TYPE timestamp(6) with time zone
        USING last_login AT TIME ZONE 'UTC',
    ALTER COLUMN created_at TYPE timestamp(6) with time zone
        USING created_at AT TIME ZONE 'UTC',
    ALTER COLUMN updated_at TYPE timestamp(6) with time zone
        USING updated_at AT TIME ZONE 'UTC',
    ALTER COLUMN password_changed_at TYPE timestamp(6) with time zone
        USING password_changed_at AT TIME ZONE 'UTC',
    ALTER COLUMN last_failed_login TYPE timestamp(6) with time zone
        USING last_failed_login AT TIME ZONE 'UTC';

-- =============================================================================
-- 3) refresh_tokens: eliminar columna redundante y alinear timestamps
-- =============================================================================

ALTER TABLE public.refresh_tokens
    DROP COLUMN IF EXISTS expiry_date;

ALTER TABLE public.refresh_tokens
    ALTER COLUMN expires_at TYPE timestamp(6) with time zone
        USING expires_at AT TIME ZONE 'UTC',
    ALTER COLUMN created_at TYPE timestamp(6) with time zone
        USING created_at AT TIME ZONE 'UTC';

-- =============================================================================
-- 4) password_reset_tokens: eliminar columna redundante y alinear timestamps
-- =============================================================================

ALTER TABLE public.password_reset_tokens
    DROP COLUMN IF EXISTS expiry_date;

ALTER TABLE public.password_reset_tokens
    ALTER COLUMN expires_at TYPE timestamp(6) with time zone
        USING expires_at AT TIME ZONE 'UTC',
    ALTER COLUMN created_at TYPE timestamp(6) with time zone
        USING created_at AT TIME ZONE 'UTC';

-- =============================================================================
-- 5) user_sessions: alinear timestamps usados por login
-- =============================================================================

ALTER TABLE public.user_sessions
    ALTER COLUMN login_at TYPE timestamp(6) with time zone
        USING login_at AT TIME ZONE 'UTC',
    ALTER COLUMN logout_at TYPE timestamp(6) with time zone
        USING logout_at AT TIME ZONE 'UTC';

-- created_at y last_accessed se mantienen para uso futuro de persistencia de sesiones
ALTER TABLE public.user_sessions
    ALTER COLUMN created_at TYPE timestamp(6) with time zone
        USING created_at AT TIME ZONE 'UTC',
    ALTER COLUMN last_accessed TYPE timestamp(6) with time zone
        USING last_accessed AT TIME ZONE 'UTC';

-- =============================================================================
-- 6) roles: alinear created_at
-- =============================================================================

ALTER TABLE public.roles
    ALTER COLUMN created_at TYPE timestamp(6) with time zone
        USING created_at AT TIME ZONE 'UTC';

-- =============================================================================
-- 7) Limpieza: revocar refresh tokens expirados
-- =============================================================================

UPDATE public.refresh_tokens
SET revoked = true
WHERE revoked = false
  AND expires_at < CURRENT_TIMESTAMP;

COMMIT;
