CREATE TABLE donors (
    id             BIGSERIAL    PRIMARY KEY,
    full_name      VARCHAR(100) NOT NULL,
    blood_type     VARCHAR(5)   NOT NULL,
    created_at     TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMP    NOT NULL DEFAULT NOW()
);
