CREATE TABLE donations (
    id             BIGSERIAL    PRIMARY KEY,
    donor_id       BIGINT       NOT NULL REFERENCES donors(id),
    donation_date  DATE         NOT NULL,
    hospital_name  VARCHAR(150) NOT NULL,
    units          INT          NOT NULL CHECK (units > 0),
    created_at     TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_donations_donor_date ON donations (donor_id, donation_date DESC);
