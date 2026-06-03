-- Story: US-006
CREATE TABLE accounts
(
    id             UUID           NOT NULL DEFAULT gen_random_uuid(),
    account_number VARCHAR(10)    NOT NULL,
    type           VARCHAR(20)    NOT NULL,
    balance        NUMERIC(19, 2) NOT NULL DEFAULT 0.00,
    status         VARCHAR(20)    NOT NULL DEFAULT 'ACTIVE',
    customer_id    UUID           NOT NULL,
    created_at     TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ    NOT NULL DEFAULT now(),

    CONSTRAINT pk_accounts             PRIMARY KEY (id),
    CONSTRAINT uq_accounts_account_number UNIQUE (account_number),
    CONSTRAINT uq_accounts_customer_type  UNIQUE (customer_id, type),
    CONSTRAINT fk_accounts_customer    FOREIGN KEY (customer_id)
        REFERENCES customers (id) ON DELETE RESTRICT,
    CONSTRAINT chk_accounts_balance    CHECK (balance >= 0),
    CONSTRAINT chk_accounts_type       CHECK (type   IN ('CHECKING', 'SAVINGS')),
    CONSTRAINT chk_accounts_status     CHECK (status IN ('ACTIVE', 'CLOSED', 'FROZEN'))
);

CREATE INDEX idx_accounts_customer_id ON accounts (customer_id);
