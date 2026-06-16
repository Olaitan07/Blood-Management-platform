package com.blood.donor.service;

import com.blood.donor.dto.DonorRequest;
import com.blood.donor.dto.DonorResponse;

import java.util.List;

public interface DonorService {

    DonorResponse registerDonor(DonorRequest request);

    List<DonorResponse> listDonors();
}
