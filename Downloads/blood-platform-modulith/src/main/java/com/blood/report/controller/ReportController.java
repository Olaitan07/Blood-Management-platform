package com.blood.report.controller;

import com.blood.report.dto.ReportResult;
import com.blood.report.model.ReportType;
import com.blood.report.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class ReportController {

    private final ReportService reportService;

    /**
     * GET /api/reports/{type}?from=2025-01-01&to=2025-12-31
     * Returns aggregated JSON report. Max range: 365 days (400 if exceeded).
     * Zero-data ranges return rows=[] with an explanatory note — never an error.
     */
    @GetMapping("/{type}")
    public ResponseEntity<ReportResult> generate(
            @PathVariable ReportType type,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        return ResponseEntity.ok(reportService.generate(type, from, to));
    }

    /**
     * GET /api/reports/{type}/export/csv?from=2025-01-01&to=2025-12-31
     * Downloads the same report as an RFC-4180 CSV file.
     */
    @GetMapping("/{type}/export/csv")
    public ResponseEntity<String> exportCsv(
            @PathVariable ReportType type,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        String csv = reportService.exportCsv(type, from, to);
        String filename = type.name().toLowerCase() + "_" + from + "_" + to + ".csv";

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(csv);
    }
}
