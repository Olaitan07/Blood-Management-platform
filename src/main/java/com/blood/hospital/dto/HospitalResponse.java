package com.blood.hospital.dto;

import com.blood.hospital.model.Hospital;
import com.blood.hospital.model.HospitalStatus;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record HospitalResponse(
        Long id,
        String name,
        String address,
        String state,
        String city,
        String contact,
        HospitalStatus status,
        String createdAt
) {

    public static HospitalResponse from(Hospital hospital) {
        return new HospitalResponse(
                hospital.getId(),
                hospital.getName(),
                hospital.getAddress(),
                hospital.getState(),
                hospital.getCity(),
                hospital.getContact(),
                hospital.getStatus(),
                hospital.getCreatedAt() != null ? hospital.getCreatedAt().toString() : null
        );
    }
}
