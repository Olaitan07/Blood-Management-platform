CREATE TABLE notifications (
    id          BIGSERIAL    PRIMARY KEY,
    message     TEXT         NOT NULL,
    sent_at     TIMESTAMP    NOT NULL DEFAULT NOW()
);
