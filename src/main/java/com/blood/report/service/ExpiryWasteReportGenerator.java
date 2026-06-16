package com.blood.report.service;

import com.blood.inventory.reporting.ExpiryWasteDto;
import com.blood.inventory.reporting.InventoryReportPort;
import com.blood.report.dto.ReportResult;
import com.blood.report.model.ReportType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
class ExpiryWasteReportGenerator implements ReportGenerator {

    private final InventoryReportPort inventoryReportPort;

    @Override
    public ReportType type() {
        return ReportType.EXPIRY_WASTE;
    }

    @Override
    public ReportResult generate(LocalDate from, LocalDate to) {
        List<ExpiryWasteDto> waste = inventoryReportPort.expiryWaste(from, to);

        List<Map<String, Object>> rows = waste.stream()
                .<Map<String, Object>>map(w -> Map.of(
                        "hospitalId",   w.hospitalId(),
                        "hospitalName", w.hospitalName(),
                        "bloodGroup",   w.bloodGroup(),
                        "wastedUnits",  w.wastedUnits(),
                        "expiryDate",   w.expiryDate()
                )).toList();

        String note = rows.isEmpty()
                ? "No expired stock with remaining units found in the selected date range." : null;
        return new ReportResult(ReportType.EXPIRY_WASTE, from, to, rows.size(), rows, note);
    }
}
