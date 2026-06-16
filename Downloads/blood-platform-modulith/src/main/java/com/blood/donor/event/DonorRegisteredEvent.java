package com.blood.donor.event;

import java.time.Instant;

// Privacy: event carries only what listeners need — phone and address are excluded intentionally.
// Outbox guarantee: Spring Modulith persists this event in the event_publication table within the
// same transaction as the donor save. If the app crashes before delivery, the event is retried on
// restart — this is the outbox pattern provided out-of-the-box by Spring Modulith.
public record DonorRegisteredEvent(
        Long donorId,
        String fullName,
        String bloodGroup,
        Instant occurredOn
) {
}
