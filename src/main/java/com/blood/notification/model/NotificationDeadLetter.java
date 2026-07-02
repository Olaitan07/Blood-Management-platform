package com.blood.notification.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Stores notifications that exhausted all retry attempts.
 * Operators can query this table to investigate delivery failures and
 * manually re-trigger if needed — one poisoned message never blocks the queue.
 */
@Entity
@Table(name = "notification_dead_letters")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDeadLetter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "notification_id", nullable = false)
    private Notification notification;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String reason;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
