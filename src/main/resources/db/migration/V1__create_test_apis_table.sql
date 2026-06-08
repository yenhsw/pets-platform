-- V1: Create test_apis table for demonstrating all repository patterns
-- This table uses all field types that TestApi entity maps to

CREATE TABLE IF NOT EXISTS test_apis (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(100)    NOT NULL,
    email       VARCHAR(255)    NOT NULL UNIQUE,
    phone       VARCHAR(20),
    age         INTEGER,
    gender      VARCHAR(10),
    salary      DECIMAL(12, 2),
    birth_date  DATE,
    bio         VARCHAR(500),
    status      VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',
    score       DOUBLE PRECISION,
    created_at  TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    created_by  VARCHAR(100)   NOT NULL DEFAULT 'system',
    updated_by  VARCHAR(100)    NOT NULL DEFAULT 'system',
    deleted     BOOLEAN        NOT NULL DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_test_apis_email        ON test_apis(email);
CREATE INDEX IF NOT EXISTS idx_test_apis_name         ON test_apis(name);
CREATE INDEX IF NOT EXISTS idx_test_apis_status       ON test_apis(status);
CREATE INDEX IF NOT EXISTS idx_test_apis_gender       ON test_apis(gender);
CREATE INDEX IF NOT EXISTS idx_test_apis_age          ON test_apis(age);
CREATE INDEX IF NOT EXISTS idx_test_apis_deleted      ON test_apis(deleted);
CREATE INDEX IF NOT EXISTS idx_test_apis_created_at   ON test_apis(created_at);

COMMENT ON TABLE test_apis IS 'Demo table for database foundation architecture patterns';
COMMENT ON COLUMN test_apis.name IS 'Full name';
COMMENT ON COLUMN test_apis.email IS 'Unique email address';
COMMENT ON COLUMN test_apis.phone IS 'Phone number';
COMMENT ON COLUMN test_apis.age IS 'Age in years';
COMMENT ON COLUMN test_apis.gender IS 'Gender: MALE, FEMALE, OTHER';
COMMENT ON COLUMN test_apis.salary IS 'Monthly salary';
COMMENT ON COLUMN test_apis.birth_date IS 'Date of birth';
COMMENT ON COLUMN test_apis.bio IS 'Biography or description';
COMMENT ON COLUMN test_apis.status IS 'Status: ACTIVE, INACTIVE, SUSPENDED';
COMMENT ON COLUMN test_apis.score IS 'Score from 0 to 10';
