package com.blood.donor.dto;

import jakarta.validation.constraints.NotBlank;

public record DonorRequest(
        @NotBlank(message = "Full name is required") String fullName,
        @NotBlank(message = "Blood type is required") String bloodType
) {
}
