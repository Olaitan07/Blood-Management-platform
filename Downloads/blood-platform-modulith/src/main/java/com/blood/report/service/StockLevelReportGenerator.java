package com.blood.report.service;

import com.blood.inventory.reporting.InventoryReportPort;
import com.blood.inventory.reporting.StockLevelDto;
import com.blood.report.dto.ReportResult;
import com.blood.report.model.ReportType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
class StockLevelReportGenerator implements ReportGenerator {

    private final InventoryReportPort inventoryReportPort;

    @Override
    public ReportType type() {
        return ReportType.STOCK_LEVELS;
    }

    @Override
    public ReportResult generate(LocalDate from, LocalDate to) {
        List<StockLevelDto> stock = inventoryReportPort.currentStockLevels();

        List<Map<String, Object>> rows = stock.stream()
                .<Map<String, Object>>map(s -> Map.of(
                        "hospitalId",    s.hospitalId(),
                        "hospitalName",  s.hospitalName(),
                        "bloodGroup",    s.bloodGroup(),
                        "unitsAvailable", s.unitsAvailable(),
                        "unitsReserved", s.unitsReserved(),
                        "netAvailable",  s.netAvailable()
                )).toList();

        String note = rows.isEmpty() ? "No stock data available." : null;
        return new ReportResult(ReportType.STOCK_LEVELS, from, to, rows.size(), rows, note);
    }
}
