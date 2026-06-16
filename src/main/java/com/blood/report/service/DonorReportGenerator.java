package com.blood.report.service;

import com.blood.donor.reporting.DonorReportPort;
import com.blood.donor.reporting.DonorStatDto;
import com.blood.report.dto.ReportResult;
import com.blood.report.model.ReportType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
class DonorReportGenerator implements ReportGenerator {

    private final DonorReportPort donorReportPort;

    @Override
    public ReportType type() {
        return ReportType.DONORS;
    }

    @Override
    public ReportResult generate(LocalDate from, LocalDate to) {
        DonorStatDto stats = donorReportPort.stats(from, to);

        List<Map<String, Object>> rows = new ArrayList<>();
        rows.add(Map.of("metric", "Total Donors (all time)", "value", stats.totalRegistered()));
        rows.add(Map.of("metric", "Registered in Range",     "value", stats.registeredInRange()));
        stats.byBloodGroup().forEach((bg, count) ->
                rows.add(Map.of("metric", "Blood Group " + bg, "value", count)));

        return new ReportResult(ReportType.DONORS, from, to, stats.totalRegistered(), rows, stats.note());
    }
}
