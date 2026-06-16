package com.blood.transfer.events;

import java.time.Instant;

public record BloodTransferCompletedEvent(
        Long transferId,
        Long requestingHospitalId,
        Long sourceHospitalId,
        String bloodGroup,
        int quantityApproved,
        int quantityReceived,
        String completedBy,
        Instant occurredAt
) {}
