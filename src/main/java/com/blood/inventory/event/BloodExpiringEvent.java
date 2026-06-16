package com.blood.inventory.event;

import java.time.Instant;
import java.util.List;

public record BloodExpiringEvent(
        Long hospitalId,
        List<ExpiringItem> items,
        Instant occurredOn
) {
    public record ExpiringItem(Long inventoryId, String bloodGroup, int units, String expiryDate) {}
}
