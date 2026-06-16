package com.blood.donor.dto;

import com.blood.donor.model.Donor;
import com.blood.donor.model.EligibilityStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record DonorResponse(
        Long id,
        String fullName,
        String bloodGroup,
        String phone,
        EligibilityStatus eligibilityStatus,
        LocalDate eligibleFrom,
        LocalDate lastDonationDate,
        LocalDateTime createdAt
) {

    public static DonorResponse from(Donor donor, LocalDate eligibleFrom) {
        return new DonorResponse(
                donor.getId(),
                donor.getFullName(),
                donor.getBloodGroup().getValue(),
                donor.getPhone(),
                donor.getEligibilityStatus(),
                eligibleFrom,
                donor.getLastDonationDate(),
                donor.getCreatedAt()
        );
    }
}
