package com.blood.inventory.event;

import java.time.Instant;
import java.time.LocalDate;

public record BloodAddedEvent(
        Long inventoryId,
        Long hospitalId,
        String bloodGroup,
        int units,
        LocalDate expiryDate,
        Instant occurredOn
) {
}
