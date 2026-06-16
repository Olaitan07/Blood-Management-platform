package com.blood.report.service;

import com.blood.report.dto.ReportResult;
import com.blood.report.model.ReportType;

import java.time.LocalDate;

public interface ReportService {

    /** Max allowed date range in days (requests exceeding this are rejected with 400). */
    int MAX_RANGE_DAYS = 365;

    ReportResult generate(ReportType type, LocalDate from, LocalDate to);

    String exportCsv(ReportType type, LocalDate from, LocalDate to);
}
