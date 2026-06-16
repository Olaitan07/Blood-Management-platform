package com.blood.audit.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Immutable audit record.  Fields are only set at construction — no setters exposed.
 * The audit store is append-only: no UPDATE or DELETE paths exist in this codebase.
 * Ordering: occurredAt (event-embedded timestamp) is the primary ordering key;
 * id (DB sequence) serves as the tiebreaker for clock-skew scenarios.
 */
@Entity
@Table(name = "audit_records")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** e.g. "BloodTransferRequestedEvent", "DonorRegisteredEvent" */
    @Column(name = "event_type", nullable = false, length = 80)
    private String eventType;

    /** User, system principal, or "system" for scheduler-triggered events. */
    @Column(name = "actor", nullable = false, length = 255)
    private String actor;

    /** Primary entity ID involved (transferId, donorId, hospitalId, …). */
    @Column(name = "target_id", length = 100)
    private String targetId;

    /** Entity type: "TRANSFER", "DONOR", "HOSPITAL", "USER". */
    @Column(name = "target_type", length = 80)
    private String targetType;

    /** Event payload serialised as a human-readable string (not full JSON to avoid PII). */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    /** Timestamp embedded in the event — used for ordering (clock-skew safe). */
    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    /** Wall-clock time when the audit consumer received and persisted the record. */
    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;
}
