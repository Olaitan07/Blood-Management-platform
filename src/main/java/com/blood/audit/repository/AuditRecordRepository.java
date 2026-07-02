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

    @Query(
            value = """
                    SELECT * FROM audit_records
                    WHERE (:eventType IS NULL OR event_type = :eventType)
                      AND (:actor     IS NULL OR actor      = :actor)
                      AND (:targetType IS NULL OR target_type = :targetType)
                      AND (:from::timestamptz IS NULL OR occurred_at >= :from::timestamptz)
                      AND (:to::timestamptz   IS NULL OR occurred_at <= :to::timestamptz)
                    ORDER BY occurred_at DESC, id DESC
                    """,
            countQuery = """
                    SELECT count(*) FROM audit_records
                    WHERE (:eventType IS NULL OR event_type = :eventType)
                      AND (:actor     IS NULL OR actor      = :actor)
                      AND (:targetType IS NULL OR target_type = :targetType)
                      AND (:from::timestamptz IS NULL OR occurred_at >= :from::timestamptz)
                      AND (:to::timestamptz   IS NULL OR occurred_at <= :to::timestamptz)
                    """,
            nativeQuery = true)
    Page<AuditRecord> filter(
            @Param("eventType")  String eventType,
            @Param("actor")      String actor,
            @Param("targetType") String targetType,
            @Param("from")       Instant from,
            @Param("to")         Instant to,
            Pageable pageable);
}
