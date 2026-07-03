package com.blood.notification.service;

import com.blood.donor.lookup.DonorLookupPort;
import com.blood.notification.dto.DeadLetterResponse;
import com.blood.notification.dto.NotificationResponse;
import com.blood.notification.exception.NotificationNotFoundException;
import com.blood.notification.model.Notification;
import com.blood.notification.repository.NotificationDeadLetterRepository;
import com.blood.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationDeadLetterRepository deadLetterRepository;
    private final DonorLookupPort donorLookupPort;

    @Override
    @Transactional(readOnly = true)
    public List<NotificationResponse> listNotifications() {
        return notificationRepository.findAll().stream()
                .map(NotificationResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationResponse> listByHospital(Long hospitalId, Authentication auth) {
        assertOwnHospitalOrAdmin(hospitalId, auth);
        return notificationRepository.findByHospitalIdOrderBySentAtDesc(hospitalId).stream()
                .map(NotificationResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationResponse> listByDonor(Long donorId, Authentication auth) {
        assertOwnDonorOrAdmin(donorId, auth);
        return notificationRepository.findByDonorIdOrderBySentAtDesc(donorId).stream()
                .map(NotificationResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<DeadLetterResponse> listDeadLetters() {
        return deadLetterRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(DeadLetterResponse::from)
                .toList();
    }

    @Override
    @Transactional
    public NotificationResponse markRead(Long notificationId, Authentication auth) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new NotificationNotFoundException(notificationId));
        assertCanAccessNotification(notification, auth);
        notification.setRead(true);
        return NotificationResponse.from(notificationRepository.save(notification));
    }

    @Override
    @Transactional
    public void markAllReadForHospital(Long hospitalId, Authentication auth) {
        assertOwnHospitalOrAdmin(hospitalId, auth);
        notificationRepository.markAllReadByHospital(hospitalId);
    }

    @Override
    @Transactional
    public void markAllReadForDonor(Long donorId, Authentication auth) {
        assertOwnDonorOrAdmin(donorId, auth);
        notificationRepository.markAllReadByDonor(donorId);
    }

    // ── Ownership checks ────────────────────────────────────────────────────
    // Each notification row carries either a hospitalId (staff-facing, e.g.
    // transfer events) or a donorId (donor-facing, e.g. registration), rarely
    // both. Admin always bypasses; everyone else must own the specific
    // hospital/donor the notification (or the requested list) belongs to.

    private void assertOwnHospitalOrAdmin(Long hospitalId, Authentication auth) {
        if (isAdmin(auth)) return;
        Long callerHospitalId = extractHospitalIdOrNull(auth);
        if (callerHospitalId == null || !callerHospitalId.equals(hospitalId)) {
            throw new AccessDeniedException("You can only view notifications for your own hospital");
        }
    }

    private void assertOwnDonorOrAdmin(Long donorId, Authentication auth) {
        if (isAdmin(auth)) return;
        Long callerDonorId = donorLookupPort.findDonorIdByEmail(auth.getName()).orElse(null);
        if (callerDonorId == null || !callerDonorId.equals(donorId)) {
            throw new AccessDeniedException("You can only view your own notifications");
        }
    }

    private void assertCanAccessNotification(Notification notification, Authentication auth) {
        if (isAdmin(auth)) return;
        if (notification.getHospitalId() != null) {
            Long callerHospitalId = extractHospitalIdOrNull(auth);
            if (callerHospitalId != null && callerHospitalId.equals(notification.getHospitalId())) return;
        }
        if (notification.getDonorId() != null) {
            Long callerDonorId = donorLookupPort.findDonorIdByEmail(auth.getName()).orElse(null);
            if (callerDonorId != null && callerDonorId.equals(notification.getDonorId())) return;
        }
        throw new AccessDeniedException("You can only modify your own notifications");
    }

    private boolean isAdmin(Authentication auth) {
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_ADMIN"::equals);
    }

    private Long extractHospitalIdOrNull(Authentication auth) {
        if (auth == null || auth.getDetails() == null) return null;
        if (auth.getDetails() instanceof Map<?, ?> details) {
            Object hospitalId = details.get("hospitalId");
            if (hospitalId instanceof Long id) return id;
            if (hospitalId instanceof Number id) return id.longValue();
        }
        return null;
    }
}
