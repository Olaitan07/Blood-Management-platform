package com.blood.audit.service;

import com.blood.audit.dto.AuditRecordResponse;
import com.blood.audit.repository.AuditRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
class AuditServiceImpl implements AuditService {

    private final AuditRecordRepository auditRecordRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<AuditRecordResponse> query(String eventType, String actor, String targetType,
                                           Instant from, Instant to, int page, int size) {
        return auditRecordRepository
                .filter(eventType, actor, targetType, from, to, PageRequest.of(page, size))
                .map(AuditRecordResponse::from);
    }
}
