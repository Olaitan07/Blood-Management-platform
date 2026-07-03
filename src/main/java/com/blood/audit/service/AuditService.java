package com.blood.audit.service;

import com.blood.audit.dto.AuditRecordResponse;
import org.springframework.data.domain.Page;

public interface AuditService {

    /**
     * Paginated, filtered audit log.
     * All filter params are optional — null means "no filter on this field".
     * from/to are ISO-8601 instant strings (e.g. "2025-01-01T00:00:00Z"),
     * kept as String rather than Instant all the way to the repository — see
     * AuditRecordRepository for why.
     */
    Page<AuditRecordResponse> query(
            String eventType,
            String actor,
            String targetType,
            String from,
            String to,
            int page,
            int size);
}
