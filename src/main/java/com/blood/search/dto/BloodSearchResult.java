package com.blood.search.dto;

public record BloodSearchResult(
        Long hospitalId,
        String hospitalName,
        String city,
        String state,
        String bloodGroup,
        int availableUnits,
        String lastUpdated
) {}
