package com.blood.transfer.events;

import java.time.Instant;

public record BloodTransferRequestedEvent(
        Long transferId,
        Long requestingHospitalId,
        Long sourceHospitalId,
        String bloodGroup,
        int quantity,
        String requestedBy,
        Instant occurredAt
) {}
