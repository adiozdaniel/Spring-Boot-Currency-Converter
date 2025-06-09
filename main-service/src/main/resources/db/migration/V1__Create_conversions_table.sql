-- DO block to create schema and role
DO $$
BEGIN
    -- Create schema if not exists
    CREATE SCHEMA IF NOT EXISTS conversion_schema;

    -- Create application user with restricted privileges if not exists
    IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'db_username') THEN
        CREATE ROLE db_username LOGIN PASSWORD 'db_password' NOINHERIT;
    END IF;

    -- Grant minimal required privileges
    GRANT CONNECT ON DATABASE conversions_database TO db_username;
    GRANT USAGE, CREATE ON SCHEMA conversion_schema TO db_username;

END
$$;

-- Enable pgcrypto extension in the schema
CREATE EXTENSION IF NOT EXISTS "pgcrypto" WITH SCHEMA conversion_schema;

-- Create conversions table
CREATE TABLE IF NOT EXISTS conversion_schema.conversions (
    id UUID PRIMARY KEY DEFAULT conversion_schema.gen_random_uuid(),
    from_currency CHAR(3) NOT NULL CHECK (from_currency ~ '^[A-Z]{3}$'),
    to_currency CHAR(3) NOT NULL CHECK (to_currency ~ '^[A-Z]{3}$'),
    amount DECIMAL(20, 4) NOT NULL CHECK (amount > 0),
    rate DECIMAL(20, 8) NOT NULL CHECK (rate > 0),
    converted_amount DECIMAL(20, 4) NOT NULL CHECK (converted_amount > 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    modified_at TIMESTAMPTZ,
    CONSTRAINT different_currencies CHECK (from_currency <> to_currency)
);

-- Create indexes
CREATE INDEX IF NOT EXISTS idx_conversions_currency_pair 
    ON conversion_schema.conversions (from_currency, to_currency);

CREATE INDEX IF NOT EXISTS idx_conversions_created_at 
    ON conversion_schema.conversions (created_at DESC);

-- Create function to update modified_at timestamp
CREATE OR REPLACE FUNCTION conversion_schema.update_modified_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.modified_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Create trigger to automatically update modified_at on update
DROP TRIGGER IF EXISTS trg_update_modified_at ON conversion_schema.conversions;

CREATE TRIGGER trg_update_modified_at
BEFORE UPDATE ON conversion_schema.conversions
FOR EACH ROW
EXECUTE FUNCTION conversion_schema.update_modified_at();

-- Grant table permissions to the application user
GRANT SELECT, INSERT, UPDATE ON conversion_schema.conversions TO db_username;

-- Add comments for documentation
COMMENT ON TABLE conversion_schema.conversions IS 'Currency conversion records';
COMMENT ON COLUMN conversion_schema.conversions.rate IS 'Exchange rate at transaction time';
