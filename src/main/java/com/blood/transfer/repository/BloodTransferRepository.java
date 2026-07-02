package com.blood.transfer.repository;

import com.blood.transfer.model.BloodTransferRequest;
import com.blood.transfer.model.TransferStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BloodTransferRepository extends JpaRepository<BloodTransferRequest, Long> {

    boolean existsByIdempotencyKey(String idempotencyKey);

    List<BloodTransferRequest> findByRequestingHospitalId(Long hospitalId);

    List<BloodTransferRequest> findBySourceHospitalIdAndStatus(Long hospitalId, TransferStatus status);

    /** All APPROVED transfers whose request date is older than the cutoff — for the 48 h expiry job. */
    @Query("SELECT t FROM BloodTransferRequest t WHERE t.status = 'APPROVED' AND t.requestDate < :cutoff")
    List<BloodTransferRequest> findApprovedOlderThan(@Param("cutoff") LocalDateTime cutoff);

    Optional<BloodTransferRequest> findByIdAndRequestingHospitalId(Long id, Long hospitalId);

    Optional<BloodTransferRequest> findByIdAndSourceHospitalId(Long id, Long hospitalId);

    /** Used by transfer::reporting to aggregate stats over a date range. */
    List<BloodTransferRequest> findByRequestDateBetween(LocalDateTime from, LocalDateTime to);
}
