package com.blood.inventory.transfer;

import java.time.LocalDate;

/**
 * Returned by {@link TransferInventoryPort#findAndReserve} after a successful reservation.
 * The transfer module stores {@code inventoryId} on the request for later deduction/release.
 */
public record InventorySlotDto(
        Long inventoryId,
        int availableUnits,   // net available BEFORE this reservation
        LocalDate expiryDate
) {}
