package com.blood.donor.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record DonationRequest(
        @NotNull(message = "Donation date is required") LocalDate donationDate,
        @NotBlank(message = "Hospital name is required") String hospitalName,
        @Min(value = 1, message = "Units must be at least 1") int units
) {
}
