package com.blood.inventory.dto;

import com.blood.inventory.model.InventoryAuditLog;

import java.time.LocalDateTime;

public record AuditLogResponse(
        Long id,
        Long inventoryId,
        Long hospitalId,
        String bloodGroup,
        int oldUnits,
        int newUnits,
        String reason,
        String changedBy,
        LocalDateTime changedAt
) {
    public static AuditLogResponse from(InventoryAuditLog log) {
        return new AuditLogResponse(
                log.getId(),
                log.getInventoryId(),
                log.getHospitalId(),
                log.getBloodGroup(),
                log.getOldUnits(),
                log.getNewUnits(),
                log.getReason(),
                log.getChangedBy(),
                log.getChangedAt()
        );
    }
}
