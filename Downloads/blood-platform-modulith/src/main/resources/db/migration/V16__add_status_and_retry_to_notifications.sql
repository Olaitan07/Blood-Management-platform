-- Epic 7: add delivery status, recipient, and retry-tracking columns to notifications
ALTER TABLE notifications
    ADD COLUMN status          VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
    ADD COLUMN recipient       VARCHAR(255)  NULL,
    ADD COLUMN retry_count     INT           NOT NULL DEFAULT 0,
    ADD COLUMN last_attempt_at TIMESTAMP     NULL;

-- Backfill: records created before Epic 7 were stored without dispatch — mark them SENT
UPDATE notifications SET status = 'SENT' WHERE status = 'PENDING';

CREATE INDEX idx_notifications_status ON notifications (status, retry_count) WHERE status IN ('PENDING', 'FAILED');

-- Dead-letter queue: notifications that failed all retry attempts
CREATE TABLE notification_dead_letters (
    id              BIGSERIAL    PRIMARY KEY,
    notification_id BIGINT       NOT NULL REFERENCES notifications(id),
    reason          TEXT         NOT NULL,
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_ndl_notification ON notification_dead_letters(notification_id);
