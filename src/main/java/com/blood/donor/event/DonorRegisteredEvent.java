package com.blood.donor.event;

public record DonorRegisteredEvent(
        Long donorId,
        String fullName,
        String bloodType,
        String timestamp
) {
}
