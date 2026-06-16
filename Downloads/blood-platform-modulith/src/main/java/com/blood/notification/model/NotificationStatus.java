package com.blood.notification.model;

public enum NotificationStatus {
    /** Saved, not yet dispatched to any external channel. */
    PENDING,
    /** At least one channel delivered successfully. */
    SENT,
    /** All channels failed; retries remain. */
    FAILED,
    /** Exhausted all retries; moved to dead-letter table. */
    DEAD_LETTER
}
