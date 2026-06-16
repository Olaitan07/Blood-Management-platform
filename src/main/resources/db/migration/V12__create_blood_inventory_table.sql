CREATE TABLE blood_inventory (
    id               BIGSERIAL    PRIMARY KEY,
    hospital_id      BIGINT       NOT NULL REFERENCES hospitals(id),
    blood_group      VARCHAR(5)   NOT NULL,
    units_available  INT          NOT NULL CHECK (units_available >= 0),
    units_reserved   INT          NOT NULL DEFAULT 0 CHECK (units_reserved >= 0),
    expiry_date      DATE         NOT NULL,
    last_updated     TIMESTAMP    NOT NULL DEFAULT NOW(),
    version          BIGINT       NOT NULL DEFAULT 0
);

-- Fast look-up for available stock by hospital + blood group
CREATE INDEX idx_inventory_hospital_group ON blood_inventory (hospital_id, blood_group);
-- Fast expiry-monitoring queries
CREATE INDEX idx_inventory_expiry ON blood_inventory (expiry_date);
