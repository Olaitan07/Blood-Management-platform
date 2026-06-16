package com.blood.transfer.reporting;

import java.time.LocalDate;

public interface TransferReportPort {
    TransferSummaryDto summarise(LocalDate from, LocalDate to);
}
