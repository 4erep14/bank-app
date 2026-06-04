-- Story: US-010
CREATE TABLE transactions
(
    id                     UUID           NOT NULL DEFAULT gen_random_uuid(),
    customer_id            UUID           NOT NULL,
    type                   VARCHAR(30)    NOT NULL,
    status                 VARCHAR(30)    NOT NULL,
    amount                 NUMERIC(19, 2) NOT NULL,
    source_account_id      UUID           NOT NULL,
    destination_account_id UUID           NOT NULL,
    description            VARCHAR(255),
    timestamp              TIMESTAMPTZ    NOT NULL DEFAULT now(),
    created_at             TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at             TIMESTAMPTZ    NOT NULL DEFAULT now(),

    CONSTRAINT pk_transactions PRIMARY KEY (id),
    CONSTRAINT fk_transactions_customer FOREIGN KEY (customer_id)
        REFERENCES customers (id) ON DELETE RESTRICT,
    CONSTRAINT fk_transactions_source_account FOREIGN KEY (source_account_id)
        REFERENCES accounts (id) ON DELETE RESTRICT,
    CONSTRAINT fk_transactions_destination_account FOREIGN KEY (destination_account_id)
        REFERENCES accounts (id) ON DELETE RESTRICT,
    CONSTRAINT chk_transactions_amount CHECK (amount > 0),
    CONSTRAINT chk_transactions_type CHECK (type IN ('TRANSFER')),
    CONSTRAINT chk_transactions_status CHECK (status IN ('PENDING_EVALUATION', 'COMPLETED', 'BLOCKED')),
    CONSTRAINT chk_transactions_accounts_differ CHECK (source_account_id <> destination_account_id)
);

CREATE INDEX idx_transactions_customer_timestamp
    ON transactions (customer_id, timestamp DESC);
CREATE INDEX idx_transactions_source_account
    ON transactions (source_account_id);
CREATE INDEX idx_transactions_destination_account
    ON transactions (destination_account_id);
CREATE INDEX idx_transactions_status
    ON transactions (status);
CREATE INDEX idx_transactions_type_timestamp
    ON transactions (type, timestamp DESC);
