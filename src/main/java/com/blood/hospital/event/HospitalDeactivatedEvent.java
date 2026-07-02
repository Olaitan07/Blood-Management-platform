package com.blood.hospital.event;

import java.time.LocalDateTime;

public record HospitalDeactivatedEvent(
        Long hospitalId,
        String name,
        String city,
        LocalDateTime deactivatedAt
) {
}
