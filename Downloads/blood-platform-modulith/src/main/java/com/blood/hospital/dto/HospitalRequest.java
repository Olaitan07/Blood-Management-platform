package com.blood.hospital.dto;

import jakarta.validation.constraints.NotBlank;

public record HospitalRequest(
        @NotBlank(message = "Name is required") String name,
        @NotBlank(message = "Address is required") String address,
        @NotBlank(message = "State is required") String state,
        @NotBlank(message = "City is required") String city,
        @NotBlank(message = "Contact is required") String contact
) {
}
