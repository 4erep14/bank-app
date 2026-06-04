-- Story: US-014 / US-015 / US-016 / US-017 / US-018
ALTER TABLE transactions DROP CONSTRAINT chk_transactions_status;
ALTER TABLE transactions
    ADD CONSTRAINT chk_transactions_status
        CHECK (status IN ('PENDING_EVALUATION', 'COMPLETED', 'BLOCKED', 'REJECTED'));

CREATE TABLE fraud_rules
(
    id                 UUID          NOT NULL DEFAULT gen_random_uuid(),
    name               VARCHAR(120)  NOT NULL,
    condition_type     VARCHAR(40)   NOT NULL,
    threshold_value    VARCHAR(40)   NOT NULL,
    active             BOOLEAN       NOT NULL DEFAULT TRUE,
    status             VARCHAR(20)   NOT NULL DEFAULT 'ACTIVE',
    created_at         TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ   NOT NULL DEFAULT now(),

    CONSTRAINT pk_fraud_rules PRIMARY KEY (id),
    CONSTRAINT uq_fraud_rules_name UNIQUE (name),
    CONSTRAINT chk_fraud_rules_condition_type CHECK (condition_type IN ('AMOUNT_EXCEEDS', 'TRANSACTION_FREQUENCY', 'UNUSUAL_HOUR')),
    CONSTRAINT chk_fraud_rules_status CHECK (status IN ('ACTIVE', 'DELETED'))
);

CREATE INDEX idx_fraud_rules_active_status
    ON fraud_rules (active, status);

CREATE TABLE fraud_alerts
(
    id                  UUID          NOT NULL DEFAULT gen_random_uuid(),
    transaction_id      UUID          NOT NULL,
    rule_name           VARCHAR(120)  NOT NULL,
    rule_condition_type VARCHAR(40)   NOT NULL,
    threshold_value     VARCHAR(40)   NOT NULL,
    actual_value        VARCHAR(80)   NOT NULL,
    timestamp           TIMESTAMPTZ   NOT NULL DEFAULT now(),
    review_status       VARCHAR(30)   NOT NULL DEFAULT 'PENDING_REVIEW',
    reviewed_by         UUID,
    reviewed_at         TIMESTAMPTZ,
    created_at          TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ   NOT NULL DEFAULT now(),

    CONSTRAINT pk_fraud_alerts PRIMARY KEY (id),
    CONSTRAINT fk_fraud_alerts_transaction FOREIGN KEY (transaction_id)
        REFERENCES transactions (id) ON DELETE RESTRICT,
    CONSTRAINT chk_fraud_alerts_condition_type CHECK (rule_condition_type IN ('AMOUNT_EXCEEDS', 'TRANSACTION_FREQUENCY', 'UNUSUAL_HOUR')),
    CONSTRAINT chk_fraud_alerts_review_status CHECK (review_status IN ('PENDING_REVIEW', 'REVIEWED'))
);

CREATE INDEX idx_fraud_alerts_timestamp
    ON fraud_alerts (timestamp DESC);
CREATE INDEX idx_fraud_alerts_review_status
    ON fraud_alerts (review_status);
CREATE INDEX idx_fraud_alerts_rule_condition_type
    ON fraud_alerts (rule_condition_type);
CREATE INDEX idx_fraud_alerts_transaction
    ON fraud_alerts (transaction_id);

CREATE TABLE notifications
(
    id                   UUID           NOT NULL DEFAULT gen_random_uuid(),
    customer_id          UUID           NOT NULL,
    type                 VARCHAR(40)    NOT NULL,
    transaction_id       UUID           NOT NULL,
    amount               NUMERIC(19, 2) NOT NULL,
    timestamp            TIMESTAMPTZ    NOT NULL DEFAULT now(),
    triggered_rule_name  VARCHAR(120)   NOT NULL,
    status               VARCHAR(20)    NOT NULL DEFAULT 'SENT',
    created_at           TIMESTAMPTZ    NOT NULL DEFAULT now(),

    CONSTRAINT pk_notifications PRIMARY KEY (id),
    CONSTRAINT fk_notifications_customer FOREIGN KEY (customer_id)
        REFERENCES customers (id) ON DELETE RESTRICT,
    CONSTRAINT fk_notifications_transaction FOREIGN KEY (transaction_id)
        REFERENCES transactions (id) ON DELETE RESTRICT,
    CONSTRAINT chk_notifications_type CHECK (type IN ('TRANSACTION_BLOCKED')),
    CONSTRAINT chk_notifications_status CHECK (status IN ('SENT'))
);

CREATE INDEX idx_notifications_customer_timestamp
    ON notifications (customer_id, timestamp DESC);
