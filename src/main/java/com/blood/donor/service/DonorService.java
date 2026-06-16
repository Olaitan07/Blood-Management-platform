package com.blood.donor.service;

import com.blood.donor.dto.DonationRequest;
import com.blood.donor.dto.DonationResponse;
import com.blood.donor.dto.DonorRequest;
import com.blood.donor.dto.DonorResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface DonorService {

    DonorResponse registerDonor(DonorRequest request, String currentUserEmail);

    DonorResponse getMyProfile(String currentUserEmail);

    List<DonorResponse> listDonors();

    DonationResponse recordDonation(Long donorId, DonationRequest request, String currentUserEmail);

    Page<DonationResponse> getDonationHistory(Long donorId, String currentUserEmail, Pageable pageable);
}
