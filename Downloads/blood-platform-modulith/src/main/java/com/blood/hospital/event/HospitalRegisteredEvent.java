package com.blood.hospital.event;

import java.time.LocalDateTime;

public record HospitalRegisteredEvent(
        Long hospitalId,
        String name,
        String city,
        String state,
        String contact,
        LocalDateTime registeredAt
) {
}
