package com.blood.donor.repository;

import com.blood.donor.model.Donor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DonorRepository extends JpaRepository<Donor, Long> {

    boolean existsByPhone(String phone);

    Optional<Donor> findByUserEmail(String userEmail);
}
