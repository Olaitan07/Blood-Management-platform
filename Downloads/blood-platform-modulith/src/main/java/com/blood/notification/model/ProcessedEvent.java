package com.blood.notification.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Tracks processed event IDs to guarantee idempotency.
 * When Kafka (or any at-least-once transport) re-delivers an event,
 * the listener checks this table before acting — ensuring only ONE
 * welcome SMS is sent per donor registration.
 */
@Entity
@Table(name = "processed_events")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessedEvent {

    @Id
    @Column(name = "event_key", nullable = false, length = 100)
    private String eventKey;

    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;
}
