package com.blood.inventory.event;

import java.time.Instant;

public record BloodUpdatedEvent(
        Long inventoryId,
        Long hospitalId,
        String bloodGroup,
        int oldUnits,
        int newUnits,
        String reason,
        String changedBy,
        Instant occurredOn
) {
}
