package com.blood.audit.repository;

import com.blood.audit.model.AuditRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;

@Repository
public interface AuditRecordRepository extends JpaRepository<AuditRecord, Long> {

    @Query("""
            SELECT a FROM AuditRecord a
            WHERE (:eventType IS NULL OR a.eventType = :eventType)
              AND (:actor     IS NULL OR a.actor     = :actor)
              AND (:targetType IS NULL OR a.targetType = :targetType)
              AND (:from IS NULL OR a.occurredAt >= :from)
              AND (:to   IS NULL OR a.occurredAt <= :to)
            ORDER BY a.occurredAt DESC, a.id DESC
            """)
    Page<AuditRecord> filter(
            @Param("eventType")  String eventType,
            @Param("actor")      String actor,
            @Param("targetType") String targetType,
            @Param("from")       Instant from,
            @Param("to")         Instant to,
            Pageable pageable);
}
