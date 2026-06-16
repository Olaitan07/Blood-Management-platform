package com.blood.donor.reporting;

import java.util.Map;

public record DonorStatDto(
        long totalRegistered,
        Map<String, Long> byBloodGroup,
        long registeredInRange,
        String note
) {}
