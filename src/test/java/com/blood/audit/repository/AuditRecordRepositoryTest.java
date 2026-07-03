package com.blood.audit.repository;

import com.blood.audit.model.AuditRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * NOTE ON TEST HISTORY: this repository's filter() query was rewritten twice
 * this session. First to fix a real bug — on real PostgreSQL, binding null
 * from/to Instant values into a native query with an "IS NULL OR <cast-expr>"
 * pattern threw "could not determine data type of parameter $N" (Postgres's
 * JDBC driver can't infer a bind parameter's type from a bare IS NULL check
 * with no other operator context); the fix binds from/to as String instead,
 * with the actual comparison cast explicitly to a timestamp type.
 * <p>
 * Writing *this* test then surfaced a second, separate issue: the cast used
 * Postgres's "timestamptz" shorthand, which H2 (even in PostgreSQL
 * compatibility mode — this project's whole test suite is H2-only, per
 * CLAUDE.md/application-test.yml, no Testcontainers/real Postgres available)
 * doesn't recognize at all ("Unknown data type: TIMESTAMPTZ"), making the
 * query completely untestable under this project's conventions. Spelling it
 * out as the ANSI-standard "timestamp with time zone" — semantically
 * identical on real Postgres — fixed that too, so this test now runs the
 * actual production query end-to-end (not a simplified stand-in), including
 * the exact all-null-params case that used to crash on Postgres.
 */
@DataJpaTest
@ActiveProfiles("test")
class AuditRecordRepositoryTest {

    @Autowired
    private AuditRecordRepository repository;

    @BeforeEach
    void seedData() {
        repository.save(record("BloodTransferRequestedEvent", "clinician@test.com", "TRANSFER", "1",
                Instant.parse("2026-01-15T10:00:00Z")));
        repository.save(record("UserRegisteredEvent", "admin#1", "USER", "5",
                Instant.parse("2026-03-10T08:30:00Z")));
        repository.save(record("HospitalDeactivatedEvent", "admin#1", "HOSPITAL", "7",
                Instant.parse("2026-06-20T14:00:00Z")));
        repository.save(record("BloodTransferApprovedEvent", "officer@test.com", "TRANSFER", "1",
                Instant.parse("2026-06-25T09:15:00Z")));
    }

    private AuditRecord record(String eventType, String actor, String targetType, String targetId, Instant occurredAt) {
        return AuditRecord.builder()
                .eventType(eventType)
                .actor(actor)
                .targetType(targetType)
                .targetId(targetId)
                .payload("test payload")
                .occurredAt(occurredAt)
                .receivedAt(occurredAt)
                .build();
    }

    @Test
    void filter_allParamsNull_returnsEveryRecord_withoutThrowing() {
        // This exact call — every filter param null — is what crashed on
        // Postgres before the fix (the screen's default, filterless state).
        var page = repository.filter(null, null, null, null, null, PageRequest.of(0, 20));

        assertThat(page.getTotalElements()).isEqualTo(4);
        // Verify ordering too: occurred_at DESC, id DESC.
        assertThat(page.getContent()).extracting(AuditRecord::getEventType)
                .containsExactly(
                        "BloodTransferApprovedEvent",   // 2026-06-25
                        "HospitalDeactivatedEvent",      // 2026-06-20
                        "UserRegisteredEvent",           // 2026-03-10
                        "BloodTransferRequestedEvent");  // 2026-01-15
    }

    @Test
    void filter_byEventType_returnsOnlyMatchingRecords() {
        var page = repository.filter("UserRegisteredEvent", null, null, null, null, PageRequest.of(0, 20));

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().get(0).getActor()).isEqualTo("admin#1");
    }

    @Test
    void filter_byActor_exactMatchOnly() {
        var page = repository.filter(null, "admin#1", null, null, null, PageRequest.of(0, 20));

        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent()).extracting(AuditRecord::getActor).containsOnly("admin#1");
    }

    @Test
    void filter_byActor_noPartialMatch() {
        // Confirms exact-match semantics — a substring of a real actor value
        // should NOT match, unlike a LIKE-based search.
        var page = repository.filter(null, "admin", null, null, null, PageRequest.of(0, 20));

        assertThat(page.getTotalElements()).isZero();
    }

    @Test
    void filter_byTargetType_returnsOnlyMatchingRecords() {
        var page = repository.filter(null, null, "HOSPITAL", null, null, PageRequest.of(0, 20));

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().get(0).getEventType()).isEqualTo("HospitalDeactivatedEvent");
    }

    @Test
    void filter_byDateRange_includesOnlyRecordsWithinBounds() {
        // Feb through end of April 2026 — should only catch the March record.
        var page = repository.filter(null, null, null,
                "2026-02-01T00:00:00Z", "2026-04-30T23:59:59Z", PageRequest.of(0, 20));

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().get(0).getEventType()).isEqualTo("UserRegisteredEvent");
    }

    @Test
    void filter_byOnlyFromDate_returnsEverythingOnOrAfter() {
        var page = repository.filter(null, null, null, "2026-06-01T00:00:00Z", null, PageRequest.of(0, 20));

        assertThat(page.getTotalElements()).isEqualTo(2);
    }

    @Test
    void filter_combinesEventTypeAndDateRange_withAndLogic() {
        // A transfer-requested event exists in January, and a transfer-approved
        // event exists in June — filtering by type=Requested AND a June-only
        // range should match neither (AND, not OR).
        var page = repository.filter("BloodTransferRequestedEvent", null, null,
                "2026-06-01T00:00:00Z", "2026-06-30T23:59:59Z", PageRequest.of(0, 20));

        assertThat(page.getTotalElements()).isZero();
    }

    @Test
    void filter_noMatches_returnsEmptyPageNotError() {
        var page = repository.filter("NoSuchEvent", null, null, null, null, PageRequest.of(0, 20));

        assertThat(page.getTotalElements()).isZero();
        assertThat(page.getContent()).isEmpty();
    }
}
