-- Story: US-001 — Customer Self-Registration
-- Flyway migration V1: create the customers table

CREATE TABLE customers
(
    id             UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    first_name     VARCHAR(100) NOT NULL,
    last_name      VARCHAR(100) NOT NULL,
    email          VARCHAR(255) NOT NULL,
    phone_number   VARCHAR(20)  NOT NULL,
    date_of_birth  DATE         NOT NULL,
    password_hash  VARCHAR(72)  NOT NULL,
    status         VARCHAR(30)  NOT NULL    DEFAULT 'PENDING_VERIFICATION',
    created_at     TIMESTAMPTZ  NOT NULL    DEFAULT now(),
    updated_at     TIMESTAMPTZ  NOT NULL    DEFAULT now(),

    CONSTRAINT uq_customers_email UNIQUE (email),
    CONSTRAINT chk_customers_status
        CHECK (status IN ('PENDING_VERIFICATION', 'ACTIVE', 'SUSPENDED', 'CLOSED'))
);

COMMENT ON TABLE  customers                IS 'Registered banking customers — US-001';
COMMENT ON COLUMN customers.password_hash  IS 'BCrypt(strength=12) hash. Plaintext is never stored.';
COMMENT ON COLUMN customers.status         IS 'CustomerStatus enum. Initial value: PENDING_VERIFICATION.';
