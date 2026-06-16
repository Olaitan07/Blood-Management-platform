package com.blood.inventory.dto;

import com.blood.inventory.model.BloodInventory;
import com.blood.inventory.model.InventoryStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record InventoryResponse(
        Long id,
        Long hospitalId,
        String bloodGroup,
        int unitsAvailable,
        int unitsReserved,
        LocalDate expiryDate,
        InventoryStatus status,
        LocalDateTime lastUpdated,
        boolean shelfLifeWarning
) {
    public static InventoryResponse from(BloodInventory inv, LocalDate today) {
        InventoryStatus status = inv.computeStatus(today);
        return new InventoryResponse(
                inv.getId(),
                inv.getHospitalId(),
                inv.getBloodGroup().getValue(),
                inv.getUnitsAvailable(),
                inv.getUnitsReserved(),
                inv.getExpiryDate(),
                status,
                inv.getLastUpdated(),
                false
        );
    }

    public static InventoryResponse withWarning(BloodInventory inv, LocalDate today) {
        InventoryStatus status = inv.computeStatus(today);
        return new InventoryResponse(
                inv.getId(),
                inv.getHospitalId(),
                inv.getBloodGroup().getValue(),
                inv.getUnitsAvailable(),
                inv.getUnitsReserved(),
                inv.getExpiryDate(),
                status,
                inv.getLastUpdated(),
                true
        );
    }
}
