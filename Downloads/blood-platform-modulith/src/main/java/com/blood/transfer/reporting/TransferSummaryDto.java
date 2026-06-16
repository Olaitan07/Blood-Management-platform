package com.blood.transfer.reporting;

import java.util.Map;

public record TransferSummaryDto(
        long total,
        Map<String, Long> byStatus,
        Double avgApprovalMinutes,  // null if no completed transfers in range
        String note                  // non-null when range has zero data
) {}
