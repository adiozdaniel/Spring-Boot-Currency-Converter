-- Create schema (H2 and PostgreSQL compatible)
CREATE SCHEMA IF NOT EXISTS conversion_schema;

-- Create conversions table (H2 compatible version)
CREATE TABLE IF NOT EXISTS conversion_schema.conversions (
    id UUID DEFAULT RANDOM_UUID() PRIMARY KEY,
    from_currency CHAR(3) NOT NULL,
    to_currency CHAR(3) NOT NULL,
    amount DECIMAL(20, 4) NOT NULL,
    rate DECIMAL(20, 8) NOT NULL,
    converted_amount DECIMAL(20, 4) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    modified_at TIMESTAMP,
    CHECK (LENGTH(from_currency) = 3),
    CHECK (LENGTH(to_currency) = 3),
    CHECK (amount > 0),
    CHECK (rate > 0),
    CHECK (converted_amount > 0),
    CHECK (from_currency <> to_currency)
);

-- Create indexes
CREATE INDEX IF NOT EXISTS idx_conversions_currency_pair
    ON conversion_schema.conversions (from_currency, to_currency);

CREATE INDEX IF NOT EXISTS idx_conversions_created_at
    ON conversion_schema.conversions (created_at DESC);
