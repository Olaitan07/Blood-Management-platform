package com.blood.inventory.security;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;

import java.util.Map;

/**
 * Extracts hospital-scoped claims from the JWT authentication token.
 * The hospitalId is stored in the Authentication details as a Map by JwtAuthenticationFilter,
 * keeping this module decoupled from auth.model.User.
 */
public final class SecurityUtils {

    private SecurityUtils() {}

    public static Long extractHospitalId(Authentication auth) {
        if (auth == null || auth.getDetails() == null) {
            throw new AccessDeniedException("No authentication context available");
        }
        if (auth.getDetails() instanceof Map<?, ?> details) {
            Object hospitalId = details.get("hospitalId");
            if (hospitalId instanceof Long id) return id;
            if (hospitalId instanceof Integer id) return id.longValue();
            if (hospitalId instanceof Number id) return id.longValue();
        }
        throw new AccessDeniedException("Hospital scope not found in token — only OFFICER/CLINICIAN roles may access inventory");
    }
}
