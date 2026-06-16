package com.blood.inventory.repository;

import com.blood.inventory.model.BloodGroup;
import com.blood.inventory.model.BloodInventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface BloodInventoryRepository extends JpaRepository<BloodInventory, Long> {

    // Only available (non-expired) records for a hospital — expiry checked at query time
    @Query("SELECT i FROM BloodInventory i WHERE i.hospitalId = :hospitalId AND i.expiryDate >= :today")
    List<BloodInventory> findAvailableByHospital(@Param("hospitalId") Long hospitalId,
                                                  @Param("today") LocalDate today);

    // Expiring within the next 7 days (for monitoring job)
    @Query("SELECT i FROM BloodInventory i WHERE i.expiryDate BETWEEN :today AND :threshold AND i.unitsAvailable > 0")
    List<BloodInventory> findExpiringSoon(@Param("today") LocalDate today,
                                          @Param("threshold") LocalDate threshold);

    // Expired units with stock remaining (for cleanup job)
    @Query("SELECT i FROM BloodInventory i WHERE i.expiryDate < :today AND i.unitsAvailable > 0")
    List<BloodInventory> findExpiredWithStock(@Param("today") LocalDate today);

    Optional<BloodInventory> findByIdAndHospitalId(Long id, Long hospitalId);

    List<BloodInventory> findByHospitalId(Long hospitalId);

    // Cross-hospital search — used by the inventory::search named interface
    @Query("SELECT i FROM BloodInventory i " +
           "WHERE i.hospitalId IN :hospitalIds " +
           "AND i.bloodGroup = :bloodGroup " +
           "AND i.expiryDate >= :today " +
           "AND (i.unitsAvailable - i.unitsReserved) > 0")
    List<BloodInventory> findAvailableByBloodGroupAcrossHospitals(
            @Param("bloodGroup") BloodGroup bloodGroup,
            @Param("hospitalIds") List<Long> hospitalIds,
            @Param("today") LocalDate today);
}
