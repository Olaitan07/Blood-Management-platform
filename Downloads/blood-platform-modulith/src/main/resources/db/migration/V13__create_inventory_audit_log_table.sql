CREATE TABLE inventory_audit_log (
    id            BIGSERIAL    PRIMARY KEY,
    inventory_id  BIGINT       NOT NULL REFERENCES blood_inventory(id),
    hospital_id   BIGINT       NOT NULL,
    blood_group   VARCHAR(5)   NOT NULL,
    old_units     INT          NOT NULL,
    new_units     INT          NOT NULL,
    reason        VARCHAR(500) NOT NULL,
    changed_by    VARCHAR(100) NOT NULL,
    changed_at    TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_audit_inventory ON inventory_audit_log (inventory_id);
CREATE INDEX idx_audit_hospital   ON inventory_audit_log (hospital_id, changed_at DESC);
