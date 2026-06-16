package com.blood.donor.dto;

import com.blood.donor.model.Donor;

public record DonorResponse(
        Long id,
        String fullName,
        String bloodType,
        String createdAt
) {

    public static DonorResponse from(Donor donor) {
        return new DonorResponse(
                donor.getId(),
                donor.getFullName(),
                donor.getBloodType(),
                donor.getCreatedAt() != null ? donor.getCreatedAt().toString() : null
        );
    }
}
