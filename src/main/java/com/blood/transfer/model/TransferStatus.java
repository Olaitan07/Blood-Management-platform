package com.blood.transfer.model;

/**
 * State machine for a BloodTransferRequest:
 *
 *   PENDING ──► APPROVED ──► COMPLETED
 *      │            │
 *      │            └──► CANCELLED  (reservation released)
 *      │            └──► EXPIRED    (48 h deadline — reservation released)
 *      └──► REJECTED
 *      └──► CANCELLED
 *      └──► INSUFFICIENT_STOCK  (approval failed mid-flight — terminal)
 *
 * Allowed transitions:
 *   PENDING          → APPROVED, REJECTED, CANCELLED
 *   APPROVED         → COMPLETED, CANCELLED, EXPIRED
 *   (all others)     → terminal, no further transitions permitted
 */
public enum TransferStatus {
    PENDING,
    APPROVED,
    REJECTED,
    CANCELLED,
    COMPLETED,
    EXPIRED,
    INSUFFICIENT_STOCK;

    public boolean isTerminal() {
        return this == REJECTED || this == CANCELLED || this == COMPLETED
                || this == EXPIRED || this == INSUFFICIENT_STOCK;
    }

    public boolean canTransitionTo(TransferStatus next) {
        return switch (this) {
            case PENDING  -> next == APPROVED || next == REJECTED || next == CANCELLED
                             || next == INSUFFICIENT_STOCK;
            case APPROVED -> next == COMPLETED || next == CANCELLED || next == EXPIRED;
            default       -> false;
        };
    }
}
