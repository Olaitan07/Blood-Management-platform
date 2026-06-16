package com.blood.inventory.event;

import java.time.Instant;
import java.time.LocalDate;

public record BloodExpiredEvent(
        Long inventoryId,
        Long hospitalId,
        String bloodGroup,
        int units,
        LocalDate expiryDate,
        Instant occurredOn
) {
}
