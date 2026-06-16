package com.blood.donor.service;

import com.blood.donor.model.EligibilityStatus;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;

/**
 * Computes donor eligibility based on last donation date.
 * All dates are evaluated in UTC (via the injected Clock) to avoid off-by-one-day
 * errors from timezone differences between server and client.
 * Boundary is inclusive: exactly 3 months after donation = ELIGIBLE.
 */
@Component
class EligibilityCalculator {

    private final Clock clock;

    EligibilityCalculator(Clock clock) {
        this.clock = clock;
    }

    EligibilityStatus compute(LocalDate lastDonationDate) {
        if (lastDonationDate == null) {
            return EligibilityStatus.ELIGIBLE;
        }
        LocalDate today = LocalDate.now(clock);
        // Inclusive boundary: donated exactly 3 months ago → eligible
        return lastDonationDate.plusMonths(3).isAfter(today)
                ? EligibilityStatus.NOT_ELIGIBLE
                : EligibilityStatus.ELIGIBLE;
    }

    /** Returns the date the donor becomes eligible again, or null if already eligible. */
    LocalDate eligibleFrom(LocalDate lastDonationDate) {
        if (lastDonationDate == null || compute(lastDonationDate) == EligibilityStatus.ELIGIBLE) {
            return null;
        }
        return lastDonationDate.plusMonths(3);
    }
}
