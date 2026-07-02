package com.blood.inventory.dto;

import com.blood.inventory.model.BloodGroup;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record AddInventoryRequest(
        @NotNull(message = "Blood group is required") BloodGroup bloodGroup,
        @Min(value = 1, message = "Units must be a positive integer") int units,
        @NotNull(message = "Expiry date is required") LocalDate expiryDate,
        boolean confirmShelfLife  // must be true when expiryDate > 42 days from today
) {
}
