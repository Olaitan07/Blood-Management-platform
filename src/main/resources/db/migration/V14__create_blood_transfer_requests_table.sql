CREATE TABLE blood_transfer_requests (
    id                      BIGSERIAL    PRIMARY KEY,
    requesting_hospital_id  BIGINT       NOT NULL REFERENCES hospitals(id),
    source_hospital_id      BIGINT       NOT NULL REFERENCES hospitals(id),
    blood_group             VARCHAR(5)   NOT NULL,
    quantity                INT          NOT NULL CHECK (quantity > 0),
    status                  VARCHAR(30)  NOT NULL DEFAULT 'PENDING',
    request_date            TIMESTAMP    NOT NULL DEFAULT NOW(),
    approval_date           TIMESTAMP,
    completion_date         TIMESTAMP,
    idempotency_key         VARCHAR(100) NOT NULL,
    requested_by_user_id    BIGINT       NOT NULL,
    approved_by_user_id     BIGINT,
    rejection_reason        VARCHAR(500),
    units_received          INT,
    source_inventory_id     BIGINT       REFERENCES blood_inventory(id),
    version                 BIGINT       NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX idx_transfer_idempotency  ON blood_transfer_requests (idempotency_key);
CREATE INDEX idx_transfer_requesting_hospital ON blood_transfer_requests (requesting_hospital_id, status);
CREATE INDEX idx_transfer_source_hospital     ON blood_transfer_requests (source_hospital_id, status);
CREATE INDEX idx_transfer_pending_expiry      ON blood_transfer_requests (status, request_date);
