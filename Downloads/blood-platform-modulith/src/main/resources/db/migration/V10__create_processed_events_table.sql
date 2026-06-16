-- Idempotency store for event consumers.
-- Prevents duplicate notifications when Kafka (or any at-least-once transport) re-delivers an event.
CREATE TABLE processed_events (
    event_key     VARCHAR(100) PRIMARY KEY,
    processed_at  TIMESTAMPTZ  NOT NULL
);
