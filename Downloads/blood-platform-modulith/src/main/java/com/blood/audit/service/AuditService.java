package com.blood.audit.service;

import com.blood.audit.dto.AuditRecordResponse;
import org.springframework.data.domain.Page;

import java.time.Instant;

public interface AuditService {

    /**
     * Paginated, filtered audit log.
     * All filter params are optional — null means "no filter on this field".
     */
    Page<AuditRecordResponse> query(
            String eventType,
            String actor,
            String targetType,
            Instant from,
            Instant to,
            int page,
            int size);
}
