package com.blood.transfer.events;

import java.time.Instant;

public record BloodTransferCancelledEvent(
        Long transferId,
        Long requestingHospitalId,
        Long sourceHospitalId,
        String bloodGroup,
        int quantity,
        boolean wasApproved,   // true → reservation was released
        String cancelledBy,
        Instant occurredAt
) {}
