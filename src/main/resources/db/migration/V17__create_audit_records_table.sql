-- Epic 8: immutable audit trail — append-only, no UPDATE or DELETE permitted via application
CREATE TABLE audit_records (
    id            BIGSERIAL     PRIMARY KEY,
    event_type    VARCHAR(80)   NOT NULL,
    actor         VARCHAR(255)  NOT NULL,
    target_id     VARCHAR(100),
    target_type   VARCHAR(80),
    payload       TEXT          NOT NULL,
    occurred_at   TIMESTAMP     NOT NULL,
    received_at   TIMESTAMP     NOT NULL DEFAULT NOW()
);

-- Common filter patterns: event_type filter, actor filter, date range scan
CREATE INDEX idx_audit_event_type  ON audit_records (event_type);
CREATE INDEX idx_audit_actor       ON audit_records (actor);
CREATE INDEX idx_audit_occurred_at ON audit_records (occurred_at DESC);
CREATE INDEX idx_audit_target      ON audit_records (target_type, target_id) WHERE target_id IS NOT NULL;
