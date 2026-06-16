package com.blood.report.service;

import com.blood.report.dto.ReportResult;
import com.blood.report.model.ReportType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
class ReportServiceImpl implements ReportService {

    /** OCP: inject all generators; dispatching is done by matching type(). */
    private final List<ReportGenerator> generators;
    private final CsvExportService csvExportService;

    @Override
    public ReportResult generate(ReportType type, LocalDate from, LocalDate to) {
        validateRange(from, to);
        return findGenerator(type).generate(from, to);
    }

    @Override
    public String exportCsv(ReportType type, LocalDate from, LocalDate to) {
        ReportResult result = generate(type, from, to);
        return csvExportService.toCsv(result);
    }

    private ReportGenerator findGenerator(ReportType type) {
        return generators.stream()
                .filter(g -> g.type() == type)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No generator for report type: " + type));
    }

    private void validateRange(LocalDate from, LocalDate to) {
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("'from' date must be before or equal to 'to' date.");
        }
        long days = ChronoUnit.DAYS.between(from, to);
        if (days > MAX_RANGE_DAYS) {
            throw new IllegalArgumentException(
                    "Date range exceeds the maximum of " + MAX_RANGE_DAYS + " days (" + days +
                    " days requested). Please narrow the range or export in multiple batches.");
        }
    }
}
