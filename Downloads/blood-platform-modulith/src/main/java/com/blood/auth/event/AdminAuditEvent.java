package com.blood.auth.event;

public record AdminAuditEvent(
        String action,
        Long adminId,
        Long targetUserId,
        String detail,
        String timestamp
) {
}
