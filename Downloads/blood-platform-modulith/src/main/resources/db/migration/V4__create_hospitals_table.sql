CREATE TABLE hospitals (
    id          BIGSERIAL    PRIMARY KEY,
    name        VARCHAR(150) NOT NULL,
    address     VARCHAR(255) NOT NULL,
    state       VARCHAR(100) NOT NULL,
    city        VARCHAR(100) NOT NULL,
    contact     VARCHAR(50)  NOT NULL,
    status      VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at  TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at  TIMESTAMP    NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX idx_hospitals_name_city ON hospitals (LOWER(name), LOWER(city));
CREATE INDEX idx_hospitals_status ON hospitals (status);
