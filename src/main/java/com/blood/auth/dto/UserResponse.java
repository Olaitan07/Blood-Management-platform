package com.blood.auth.dto;

import com.blood.auth.model.AccountStatus;
import com.blood.auth.model.Role;
import com.blood.auth.model.User;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record UserResponse(
        Long id,
        String name,
        String email,
        Role role,
        Long hospitalId,
        AccountStatus status,
        String createdAt
) {

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.getHospitalId(),
                user.getStatus(),
                user.getCreatedAt() != null ? user.getCreatedAt().toString() : null
        );
    }
}
