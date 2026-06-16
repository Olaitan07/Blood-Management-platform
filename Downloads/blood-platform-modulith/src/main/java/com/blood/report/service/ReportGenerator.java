package com.blood.report.service;

import com.blood.report.dto.ReportResult;
import com.blood.report.model.ReportType;

import java.time.LocalDate;

/**
 * OCP: each report type implements this interface.
 * {@link ReportServiceImpl} discovers all generators via the injected list —
 * adding a new report type never requires modifying existing code.
 */
public interface ReportGenerator {
    ReportType type();
    ReportResult generate(LocalDate from, LocalDate to);
}
