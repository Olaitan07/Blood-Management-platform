package com.blood.transfer.events;

import java.time.Instant;

public record BloodTransferApprovedEvent(
        Long transferId,
        Long requestingHospitalId,
        Long sourceHospitalId,
        String bloodGroup,
        int quantity,
        String approvedBy,
        Instant occurredAt
) {}
