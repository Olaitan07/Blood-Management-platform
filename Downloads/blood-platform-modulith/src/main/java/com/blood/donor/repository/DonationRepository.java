package com.blood.donor.repository;

import com.blood.donor.model.Donation;
import com.blood.donor.model.Donor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface DonationRepository extends JpaRepository<Donation, Long> {

    Page<Donation> findByDonorOrderByDonationDateDesc(Donor donor, Pageable pageable);

    Optional<Donation> findTopByDonorOrderByDonationDateDesc(Donor donor);

    boolean existsByDonorAndDonationDateBefore(Donor donor, LocalDate date);
}
