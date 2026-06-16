package com.blood.transfer.dto;

import jakarta.validation.constraints.Min;

public record CompleteTransferRequest(
        /** Units actually received. May be less than approved quantity (partial receipt / breakage). */
        @Min(value = 0, message = "unitsReceived cannot be negative")
        int unitsReceived
) {}
