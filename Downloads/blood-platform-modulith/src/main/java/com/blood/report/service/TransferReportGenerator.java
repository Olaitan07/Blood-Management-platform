package com.blood.report.service;

import com.blood.report.dto.ReportResult;
import com.blood.report.model.ReportType;
import com.blood.transfer.reporting.TransferReportPort;
import com.blood.transfer.reporting.TransferSummaryDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
class TransferReportGenerator implements ReportGenerator {

    private final TransferReportPort transferReportPort;

    @Override
    public ReportType type() {
        return ReportType.TRANSFERS;
    }

    @Override
    public ReportResult generate(LocalDate from, LocalDate to) {
        TransferSummaryDto summary = transferReportPort.summarise(from, to);

        // One summary row + one row per status breakdown
        List<Map<String, Object>> rows = new ArrayList<>();
        rows.add(Map.of(
                "metric", "Total Transfers",
                "value",  summary.total()
        ));
        summary.byStatus().forEach((status, count) ->
                rows.add(Map.of("metric", "Status: " + status, "value", count)));
        if (summary.avgApprovalMinutes() != null) {
            rows.add(Map.of(
                    "metric", "Avg Approval Time (minutes)",
                    "value",  Math.round(summary.avgApprovalMinutes())
            ));
        }

        return new ReportResult(ReportType.TRANSFERS, from, to, summary.total(), rows, summary.note());
    }
}
