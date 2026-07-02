ALTER TABLE notifications
    ADD COLUMN transfer_id  BIGINT       NULL,
    ADD COLUMN hospital_id  BIGINT       NULL,
    ADD COLUMN type         VARCHAR(50)  NOT NULL DEFAULT 'DONOR';

CREATE INDEX idx_notifications_hospital ON notifications (hospital_id) WHERE hospital_id IS NOT NULL;
CREATE INDEX idx_notifications_transfer ON notifications (transfer_id) WHERE transfer_id IS NOT NULL;
