package com.blood.auth.dto;

import com.blood.auth.model.Role;
import com.blood.auth.validation.HospitalRequiredForRole;
import com.blood.auth.validation.StrongPassword;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@HospitalRequiredForRole
public record RegisterRequest(
        @NotBlank(message = "Name is required") String name,
        @NotBlank(message = "Email is required") @Email(message = "Invalid email format") String email,
        @StrongPassword String password,
        @NotNull(message = "Role is required") Role role,
        Long hospitalId
) {
}
