-- Create discount_codes table
CREATE TABLE discount_codes (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(255) UNIQUE NOT NULL,
    discount_percentage INTEGER NOT NULL CHECK (discount_percentage > 0 AND discount_percentage <= 100),
    max_uses INTEGER CHECK (max_uses > 0),
    uses_count INTEGER DEFAULT 0 CHECK (uses_count >= 0),
    expires_at TIMESTAMP,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for performance
CREATE INDEX idx_discount_codes_code ON discount_codes(code);
CREATE INDEX idx_discount_codes_active ON discount_codes(is_active);
CREATE INDEX idx_discount_codes_expires_at ON discount_codes(expires_at);

-- Add comments
COMMENT ON TABLE discount_codes IS 'Stores promotional discount codes for subscriptions';
COMMENT ON COLUMN discount_codes.code IS 'Unique discount code (e.g., WELCOME20)';
COMMENT ON COLUMN discount_codes.discount_percentage IS 'Discount percentage (20 means 20% off)';
COMMENT ON COLUMN discount_codes.max_uses IS 'Maximum number of times code can be used (null = unlimited)';
COMMENT ON COLUMN discount_codes.uses_count IS 'Number of times code has been used';
COMMENT ON COLUMN discount_codes.expires_at IS 'Code expiry date (null = no expiry)';
COMMENT ON COLUMN discount_codes.is_active IS 'Whether the code is currently active';
COMMENT ON COLUMN discount_codes.created_at IS 'When the code was created';