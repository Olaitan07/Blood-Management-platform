package com.blood.notification.repository;

import com.blood.notification.model.Notification;
import com.blood.notification.model.NotificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByHospitalIdOrderBySentAtDesc(Long hospitalId);

    List<Notification> findByDonorIdOrderBySentAtDesc(Long donorId);

    /**
     * Finds PENDING or FAILED notifications that still have retries remaining.
     * The retry scheduler applies exponential-backoff timing on top of this.
     */
    @Query("SELECT n FROM Notification n WHERE n.status IN :statuses AND n.retryCount < :maxRetries")
    List<Notification> findDispatchEligible(
            @Param("statuses") List<NotificationStatus> statuses,
            @Param("maxRetries") int maxRetries);
}
