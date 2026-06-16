package com.blood.donor.event;

import java.time.Instant;
import java.time.LocalDate;

public record DonationRecordedEvent(
        Long donorId,
        String fullName,
        String bloodGroup,
        LocalDate donationDate,
        Instant occurredOn
) {
}
