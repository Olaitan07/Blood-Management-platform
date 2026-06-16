package com.blood.inventory.reporting;

import java.time.LocalDate;
import java.util.List;

public interface InventoryReportPort {
    List<StockLevelDto> currentStockLevels();
    List<ExpiryWasteDto> expiryWaste(LocalDate from, LocalDate to);
}
