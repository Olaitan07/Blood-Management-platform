package com.blood.donor.reporting;

import java.time.LocalDate;

public interface DonorReportPort {
    DonorStatDto stats(LocalDate from, LocalDate to);
}
