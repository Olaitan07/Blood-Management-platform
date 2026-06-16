package com.blood.inventory.search;

import java.time.LocalDateTime;

/**
 * Cross-module DTO exposed via the inventory::search named interface.
 * Carries available (non-expired, non-reserved) unit counts per hospital.
 */
public record AvailableBloodDto(
        Long hospitalId,
        String bloodGroup,
        int availableUnits,      // unitsAvailable − unitsReserved
        LocalDateTime lastUpdated
) {}
