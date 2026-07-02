package com.blood.donor.dto;

import com.blood.donor.model.BloodGroup;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record DonorRequest(
        @NotBlank(message = "Full name is required") String fullName,
        @NotNull(message = "Blood group is required") BloodGroup bloodGroup,
        @NotBlank(message = "Phone number is required")
        @Pattern(regexp = "^\\+?[0-9]{7,15}$", message = "Invalid phone number format") String phone
) {
}
