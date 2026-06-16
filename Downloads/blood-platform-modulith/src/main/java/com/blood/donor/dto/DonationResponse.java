package com.blood.donor.dto;

import com.blood.donor.model.Donation;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record DonationResponse(
        Long id,
        LocalDate donationDate,
        String hospitalName,
        int units,
        LocalDateTime createdAt
) {

    public static DonationResponse from(Donation donation) {
        return new DonationResponse(
                donation.getId(),
                donation.getDonationDate(),
                donation.getHospitalName(),
                donation.getUnits(),
                donation.getCreatedAt()
        );
    }
}
