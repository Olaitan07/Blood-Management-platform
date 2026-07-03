package com.blood.donor.service;

import com.blood.donor.model.EligibilityStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class EligibilityCalculatorTest {

    // Fixed "today" so LocalDate.now(clock) is deterministic: 2026-07-03
    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2026-07-03T00:00:00Z"), ZoneOffset.UTC);

    private EligibilityCalculator calculator;
    private LocalDate today;

    @BeforeEach
    void setUp() {
        calculator = new EligibilityCalculator(FIXED_CLOCK);
        today = LocalDate.now(FIXED_CLOCK);
    }

    @Test
    void compute_nullLastDonationDate_isEligible() {
        assertThat(calculator.compute(null)).isEqualTo(EligibilityStatus.ELIGIBLE);
    }

    @Test
    void compute_exactlyThreeMonthsSince_isEligible_inclusiveBoundary() {
        LocalDate lastDonation = today.minusMonths(3);
        assertThat(calculator.compute(lastDonation)).isEqualTo(EligibilityStatus.ELIGIBLE);
    }

    @Test
    void compute_oneDayShortOfThreeMonths_isNotEligible() {
        LocalDate lastDonation = today.minusMonths(3).plusDays(1);
        assertThat(calculator.compute(lastDonation)).isEqualTo(EligibilityStatus.NOT_ELIGIBLE);
    }

    @Test
    void compute_wellPastThreeMonths_isEligible() {
        LocalDate lastDonation = today.minusMonths(6);
        assertThat(calculator.compute(lastDonation)).isEqualTo(EligibilityStatus.ELIGIBLE);
    }

    @Test
    void eligibleFrom_nullLastDonationDate_returnsNull() {
        assertThat(calculator.eligibleFrom(null)).isNull();
    }

    @Test
    void eligibleFrom_alreadyEligibleDate_returnsNull() {
        LocalDate lastDonation = today.minusMonths(3);
        assertThat(calculator.eligibleFrom(lastDonation)).isNull();
    }

    @Test
    void eligibleFrom_notYetEligibleDate_returnsLastDonationPlusThreeMonths() {
        LocalDate lastDonation = today.minusMonths(3).plusDays(1);
        assertThat(calculator.eligibleFrom(lastDonation)).isEqualTo(lastDonation.plusMonths(3));
    }
}
