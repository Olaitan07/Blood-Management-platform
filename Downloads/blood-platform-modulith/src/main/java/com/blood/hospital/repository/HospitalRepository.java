package com.blood.hospital.repository;

import com.blood.hospital.model.Hospital;
import com.blood.hospital.model.HospitalStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HospitalRepository extends JpaRepository<Hospital, Long> {

    List<Hospital> findByStatus(HospitalStatus status);

    Optional<Hospital> findByNameIgnoreCaseAndCityIgnoreCase(String name, String city);
}
