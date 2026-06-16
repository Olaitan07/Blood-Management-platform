package com.blood.auth.dto;

import com.blood.auth.model.AccountStatus;
import com.blood.auth.model.Role;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AuthResponse(
        String token,
        String type,
        Long userId,
        String name,
        String email,
        Role role,
        AccountStatus accountStatus
) {

    public static AuthResponse of(String token, Long userId, String name, String email,
                                   Role role, AccountStatus status) {
        return new AuthResponse(
                token,
                token != null ? "Bearer" : null,
                userId,
                name,
                email,
                role,
                status
        );
    }
}
