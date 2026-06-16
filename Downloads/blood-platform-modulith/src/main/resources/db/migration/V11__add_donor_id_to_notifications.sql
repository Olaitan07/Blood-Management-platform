ALTER TABLE notifications
    ADD COLUMN donor_id BIGINT REFERENCES donors(id);
