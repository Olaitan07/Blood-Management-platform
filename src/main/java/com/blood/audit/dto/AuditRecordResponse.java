package com.blood.audit.dto;

import com.blood.audit.model.AuditRecord;

public record AuditRecordResponse(
        Long id,
        String eventType,
        String actor,
        String targetId,
        String targetType,
        String payload,
        String occurredAt,
        String receivedAt
) {
    public static AuditRecordResponse from(AuditRecord r) {
        return new AuditRecordResponse(
                r.getId(), r.getEventType(), r.getActor(),
                r.getTargetId(), r.getTargetType(), r.getPayload(),
                r.getOccurredAt().toString(), r.getReceivedAt().toString());
    }
}
