-- Story: US-019 / US-020
-- Adds customer deactivation support and immutable system audit entries.

ALTER TABLE customers
    DROP CONSTRAINT IF EXISTS chk_customers_status;

ALTER TABLE customers
    ADD CONSTRAINT chk_customers_status
    CHECK (status IN ('PENDING_VERIFICATION', 'ACTIVE', 'SUSPENDED', 'CLOSED', 'LOCKED', 'INACTIVE'));

CREATE TABLE audit_logs
(
    id                 UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    actor_id           UUID,
    actor_role         VARCHAR(40)  NOT NULL,
    action_type        VARCHAR(60)  NOT NULL,
    target_entity_type VARCHAR(60)  NOT NULL,
    target_entity_id   UUID,
    timestamp          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    ip_address         VARCHAR(64)
);

CREATE INDEX idx_audit_logs_actor_id ON audit_logs(actor_id);
CREATE INDEX idx_audit_logs_action_type ON audit_logs(action_type);
CREATE INDEX idx_audit_logs_timestamp ON audit_logs(timestamp DESC);

COMMENT ON TABLE audit_logs IS 'Immutable platform audit log entries — US-020';
