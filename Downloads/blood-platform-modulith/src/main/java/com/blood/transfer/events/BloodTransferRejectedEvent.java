package com.blood.transfer.events;

import java.time.Instant;

public record BloodTransferRejectedEvent(
        Long transferId,
        Long requestingHospitalId,
        Long sourceHospitalId,
        String bloodGroup,
        int quantity,
        String reason,
        String rejectedBy,
        Instant occurredAt
) {}
