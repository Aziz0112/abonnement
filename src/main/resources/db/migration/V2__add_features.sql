-- Feature 2: reminder flag
ALTER TABLE usersubscriptions ADD COLUMN IF NOT EXISTS reminder_sent BOOLEAN NOT NULL DEFAULT FALSE;

-- Feature 3: invoices table
CREATE TABLE IF NOT EXISTS invoices (
    id                BIGSERIAL PRIMARY KEY,
    invoice_number    VARCHAR(50) UNIQUE NOT NULL,
    subscription_id   BIGINT NOT NULL REFERENCES usersubscriptions(id),
    user_id           BIGINT NOT NULL,
    amount            DOUBLE PRECISION NOT NULL,
    issued_at         TIMESTAMP NOT NULL,
    renewal_date      TIMESTAMP,
    stripe_session_id VARCHAR(255),
    paid              BOOLEAN NOT NULL DEFAULT TRUE
);

-- Feature 4: auto-renew + stripe customer
ALTER TABLE usersubscriptions ADD COLUMN IF NOT EXISTS auto_renew BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE usersubscriptions ADD COLUMN IF NOT EXISTS stripe_customer_id VARCHAR(255);
