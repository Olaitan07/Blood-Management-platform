package com.blood.hospital.service;

import com.blood.hospital.dto.HospitalRequest;
import com.blood.hospital.dto.HospitalResponse;

import java.util.List;

public interface HospitalService {

    HospitalResponse registerHospital(HospitalRequest request);

    List<HospitalResponse> listActiveHospitals();

    List<HospitalResponse> listAllHospitals();

    HospitalResponse getHospitalById(Long id);

    HospitalResponse deactivateHospital(Long id);

    /**
     * Called by auth module to verify a hospital exists and is active before
     * assigning a user to it. This is the public API surface of this module.
     */
    boolean hospitalExists(Long id);
}
