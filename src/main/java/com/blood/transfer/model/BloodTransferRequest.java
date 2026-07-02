package com.blood.transfer.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "blood_transfer_requests")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BloodTransferRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "requesting_hospital_id", nullable = false)
    private Long requestingHospitalId;

    @Column(name = "source_hospital_id", nullable = false)
    private Long sourceHospitalId;

    @Column(name = "blood_group", nullable = false, length = 5)
    private String bloodGroup;

    @Column(nullable = false)
    private int quantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private TransferStatus status = TransferStatus.PENDING;

    @Column(name = "request_date", nullable = false)
    private LocalDateTime requestDate;

    @Column(name = "approval_date")
    private LocalDateTime approvalDate;

    @Column(name = "completion_date")
    private LocalDateTime completionDate;

    /** Client-supplied key — duplicate submissions within a minute are rejected. */
    @Column(name = "idempotency_key", nullable = false, unique = true, length = 100)
    private String idempotencyKey;

    @Column(name = "requested_by_user_id", nullable = false)
    private Long requestedByUserId;

    @Column(name = "approved_by_user_id")
    private Long approvedByUserId;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    /** Actual units received (may differ from quantity in partial-receipt case). */
    @Column(name = "units_received")
    private Integer unitsReceived;

    /** Set during approval — links to the inventory row that holds the reservation. */
    @Column(name = "source_inventory_id")
    private Long sourceInventoryId;

    @Version
    @Column(nullable = false)
    private Long version;

    public void transitionTo(TransferStatus next) {
        if (!this.status.canTransitionTo(next)) {
            throw new IllegalStateException(
                    "Invalid transition: " + this.status + " → " + next +
                    " for transferId=" + this.id);
        }
        this.status = next;
    }
}
