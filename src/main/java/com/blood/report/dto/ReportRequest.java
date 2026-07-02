package com.blood.report.dto;

import com.blood.report.model.ReportType;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record ReportRequest(
        @NotNull ReportType type,
        @NotNull LocalDate from,
        @NotNull LocalDate to
) {}
