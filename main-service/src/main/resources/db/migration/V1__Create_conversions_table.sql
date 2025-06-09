DO $$
BEGIN
    -- Create schema first
    CREATE SCHEMA IF NOT EXISTS conversion_schema;
    
    -- Application user with restricted privileges
    IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'db_username') THEN
        CREATE ROLE db_username LOGIN PASSWORD 'db_password' NOINHERIT;
    END IF;
    
    -- Grant minimal required privileges
    GRANT CONNECT ON DATABASE conversions_database TO db_username;
    GRANT USAGE, CREATE ON SCHEMA conversion_schema TO db_username;
END
$$;

-- Enable extensions
CREATE EXTENSION IF NOT EXISTS "pgcrypto" WITH SCHEMA conversion_schema;

-- Main table with constraints
CREATE TABLE IF NOT EXISTS conversion_schema.conversions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    from_currency CHAR(3) NOT NULL CHECK (from_currency ~ '^[A-Z]{3}$'),
    to_currency CHAR(3) NOT NULL CHECK (to_currency ~ '^[A-Z]{3}$'),
    amount DECIMAL(20, 4) NOT NULL CHECK (amount > 0),
    rate DECIMAL(20, 8) NOT NULL CHECK (rate > 0),
    converted_amount DECIMAL(20, 4) NOT NULL CHECK (converted_amount > 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    modified_at TIMESTAMPTZ,
    CONSTRAINT different_currencies CHECK (from_currency <> to_currency)
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_conversions_currency_pair 
    ON conversion_schema.conversions(from_currency, to_currency);

CREATE INDEX IF NOT EXISTS idx_conversions_created_at 
    ON conversion_schema.conversions(created_at DESC);

-- Permissions
GRANT SELECT, INSERT ON conversion_schema.conversions TO admin;

-- Table comments
COMMENT ON TABLE conversion_schema.conversions IS 'Currency conversion records';
COMMENT ON COLUMN conversion_schema.conversions.rate IS 'Exchange rate at transaction time';
