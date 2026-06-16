package com.blood.inventory.reporting;

public record ExpiryWasteDto(
        Long hospitalId,
        String hospitalName,
        String bloodGroup,
        int wastedUnits,
        String expiryDate
) {}
