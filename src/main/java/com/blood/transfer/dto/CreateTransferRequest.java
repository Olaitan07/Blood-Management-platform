package com.blood.transfer.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateTransferRequest(
        @NotNull(message = "sourceHospitalId is required")
        Long sourceHospitalId,

        @NotBlank(message = "bloodGroup is required")
        String bloodGroup,

        @Min(value = 1, message = "Quantity must be at least 1")
        int quantity,

        /** Client-generated UUID or idempotency key (max 100 chars). */
        @NotBlank(message = "idempotencyKey is required")
        @Size(max = 100, message = "idempotencyKey must not exceed 100 characters")
        String idempotencyKey
) {}
