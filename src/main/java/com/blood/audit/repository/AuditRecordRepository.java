package com.blood.audit.repository;

import com.blood.audit.model.AuditRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditRecordRepository extends JpaRepository<AuditRecord, Long> {

    // from/to are bound as String, not Instant — a null Instant bound into an
    // "IS NULL OR <cast-expr> >= ?" native query leaves Postgres unable to
    // infer that parameter's type ("could not determine data type of
    // parameter $N"), because the "IS NULL" branch alone gives the JDBC
    // driver no type context. Binding as String sidesteps this: Hibernate
    // always knows a String parameter is VARCHAR, so a null bind is
    // unambiguous, and the explicit cast only ever applies to the actual
    // comparison branch, not the null-check branch. This was the *default*,
    // filterless state of the whole screen (both dates omitted) — found live
    // while verifying the frontend, not caught by any test. The cast spells
    // out "timestamp with time zone" (not Postgres's "timestamptz" shorthand)
    // deliberately — both mean the same thing on real Postgres, but only the
    // spelled-out ANSI form is understood by H2, which is what makes this
    // query testable at all under this project's H2-only test conventions.
    @Query(
            value = """
                    SELECT * FROM audit_records
                    WHERE (:eventType IS NULL OR event_type = :eventType)
                      AND (:actor     IS NULL OR actor      = :actor)
                      AND (:targetType IS NULL OR target_type = :targetType)
                      AND (:from IS NULL OR occurred_at >= CAST(:from AS timestamp with time zone))
                      AND (:to   IS NULL OR occurred_at <= CAST(:to AS timestamp with time zone))
                    ORDER BY occurred_at DESC, id DESC
                    """,
            countQuery = """
                    SELECT count(*) FROM audit_records
                    WHERE (:eventType IS NULL OR event_type = :eventType)
                      AND (:actor     IS NULL OR actor      = :actor)
                      AND (:targetType IS NULL OR target_type = :targetType)
                      AND (:from IS NULL OR occurred_at >= CAST(:from AS timestamp with time zone))
                      AND (:to   IS NULL OR occurred_at <= CAST(:to AS timestamp with time zone))
                    """,
            nativeQuery = true)
    Page<AuditRecord> filter(
            @Param("eventType")  String eventType,
            @Param("actor")      String actor,
            @Param("targetType") String targetType,
            @Param("from")       String from,
            @Param("to")         String to,
            Pageable pageable);
}
