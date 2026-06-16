package com.blood.inventory.reporting;

public record StockLevelDto(
        Long hospitalId,
        String hospitalName,
        String bloodGroup,
        int unitsAvailable,
        int unitsReserved,
        int netAvailable
) {}
