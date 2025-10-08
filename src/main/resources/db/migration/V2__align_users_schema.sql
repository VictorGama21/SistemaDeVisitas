-- V2__align_users_schema.sql
-- Alinha a tabela users ao modelo atual (User.java)

-- 1) Completa full_name com 'name' legada (se existir)
DO $$
BEGIN
  IF EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_name = 'users' AND column_name = 'name'
  ) THEN
    EXECUTE 'UPDATE users SET full_name = COALESCE(full_name, name)';
  END IF;
END$$;

-- 2) Ajusta NOT NULL e defaults
ALTER TABLE users
  ALTER COLUMN full_name SET NOT NULL,
  ALTER COLUMN email     SET NOT NULL,
  ALTER COLUMN password  SET NOT NULL,
  ALTER COLUMN role_group SET NOT NULL,
  ALTER COLUMN enabled   SET DEFAULT TRUE,
  ALTER COLUMN enabled   SET NOT NULL,
  ALTER COLUMN created_at SET DEFAULT now();

UPDATE users SET created_at = now() WHERE created_at IS NULL;

-- 3) Recria o CHECK de role_group para SUPER/ADMIN/LOJA
DO $$
BEGIN
  IF EXISTS (
    SELECT 1
    FROM information_schema.table_constraints
    WHERE table_name = 'users'
      AND constraint_type = 'CHECK'
      AND constraint_name = 'users_role_group_check'
  ) THEN
    EXECUTE 'ALTER TABLE users DROP CONSTRAINT users_role_group_check';
  END IF;
END$$;

ALTER TABLE users
  ADD CONSTRAINT users_role_group_check
  CHECK (role_group IN ('SUPER','ADMIN','LOJA'));

-- 4) Remove colunas legadas, se ainda existirem
DO $$
BEGIN
  IF EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_name = 'users' AND column_name = 'password_hash'
  ) THEN
    EXECUTE 'ALTER TABLE users DROP COLUMN password_hash';
  END IF;

  IF EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_name = 'users' AND column_name = 'name'
  ) THEN
    EXECUTE 'ALTER TABLE users DROP COLUMN name';
  END IF;

  IF EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_name = 'users' AND column_name = 'role'
  ) THEN
    EXECUTE 'ALTER TABLE users DROP COLUMN role';
  END IF;
END$$;

-- 5) Índices úteis/únicos
CREATE UNIQUE INDEX IF NOT EXISTS uq_users_email ON users(email);
