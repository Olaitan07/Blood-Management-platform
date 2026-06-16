package com.blood.inventory.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdateInventoryRequest(
        @NotNull(message = "Corrected unit count is required") Integer units,
        @NotBlank(message = "Reason for correction is required") String reason
) {
}
