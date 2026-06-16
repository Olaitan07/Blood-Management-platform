package com.blood.report.dto;

import com.blood.report.model.ReportType;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Generic report envelope.  {@code rows} is a list of maps so each report type
 * can define its own column set without requiring a parallel DTO hierarchy.
 */
public record ReportResult(
        ReportType type,
        LocalDate from,
        LocalDate to,
        long totalRows,
        List<Map<String, Object>> rows,
        String note
) {}
