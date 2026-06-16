package com.blood.audit.controller;

import com.blood.audit.dto.AuditRecordResponse;
import com.blood.audit.service.AuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

/**
 * Audit log is READ-ONLY by design — append-only store.
 * PUT/DELETE/PATCH are explicitly rejected with 405 Method Not Allowed.
 */
@RestController
@RequestMapping("/api/audit")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AuditController {

    private final AuditService auditService;

    /**
     * GET /api/audit?eventType=&actor=&targetType=&from=&to=&page=0&size=20
     * All query params are optional.
     * Dates use ISO-8601 instant format: 2025-01-01T00:00:00Z
     */
    @GetMapping
    public ResponseEntity<Page<AuditRecordResponse>> query(
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) String actor,
            @RequestParam(required = false) String targetType,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {

        if (size > 100) size = 100;

        Instant fromInstant = from != null ? Instant.parse(from) : null;
        Instant toInstant   = to   != null ? Instant.parse(to)   : null;

        return ResponseEntity.ok(
                auditService.query(eventType, actor, targetType, fromInstant, toInstant, page, size));
    }

    // ── Append-only enforcement ────────────────────────────────────────────────

    @PutMapping
    @PatchMapping
    @DeleteMapping
    public ResponseEntity<String> rejectMutation() {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body("The audit log is append-only. Modifications are not permitted.");
    }

    @PutMapping("/{id}")
    @PatchMapping("/{id}")
    @DeleteMapping("/{id}")
    public ResponseEntity<String> rejectMutationById(@PathVariable Long id) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body("Audit record #" + id + " cannot be modified or deleted.");
    }
}
