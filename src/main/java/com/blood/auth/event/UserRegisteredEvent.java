package com.blood.auth.event;

import com.blood.auth.model.Role;

public record UserRegisteredEvent(
        Long userId,
        String name,
        String email,
        Role role,
        Long hospitalId
) {
}
