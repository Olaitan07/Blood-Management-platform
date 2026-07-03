package com.blood.notification.service;

import com.blood.donor.lookup.DonorLookupPort;
import com.blood.notification.exception.NotificationNotFoundException;
import com.blood.notification.model.Notification;
import com.blood.notification.repository.NotificationDeadLetterRepository;
import com.blood.notification.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Verifies the ownership checks added this session: /hospital/{id} and
 * /donor/{id} notification endpoints previously had no check at all — any
 * authenticated user could pass any id and read someone else's notifications.
 */
@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    private static final Long OWN_HOSPITAL_ID = 5L;
    private static final Long OTHER_HOSPITAL_ID = 99L;
    private static final Long OWN_DONOR_ID = 7L;
    private static final Long OTHER_DONOR_ID = 42L;
    private static final String CALLER_EMAIL = "caller@test.com";

    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private NotificationDeadLetterRepository deadLetterRepository;
    @Mock
    private DonorLookupPort donorLookupPort;

    private NotificationServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new NotificationServiceImpl(notificationRepository, deadLetterRepository, donorLookupPort);
    }

    private Authentication authWithHospital(Long hospitalId, String role) {
        Authentication auth = new UsernamePasswordAuthenticationToken(
                CALLER_EMAIL, null, List.of(new SimpleGrantedAuthority("ROLE_" + role)));
        ((UsernamePasswordAuthenticationToken) auth).setDetails(
                hospitalId == null ? Map.of() : Map.of("hospitalId", hospitalId));
        return auth;
    }

    // ── listByHospital ──────────────────────────────────────────────────────

    @Test
    void listByHospital_ownHospital_succeeds() {
        Authentication auth = authWithHospital(OWN_HOSPITAL_ID, "OFFICER");
        when(notificationRepository.findByHospitalIdOrderBySentAtDesc(OWN_HOSPITAL_ID)).thenReturn(List.of());

        service.listByHospital(OWN_HOSPITAL_ID, auth);

        verify(notificationRepository).findByHospitalIdOrderBySentAtDesc(OWN_HOSPITAL_ID);
    }

    @Test
    void listByHospital_someoneElsesHospital_throwsAccessDenied() {
        Authentication auth = authWithHospital(OWN_HOSPITAL_ID, "OFFICER");

        assertThatThrownBy(() -> service.listByHospital(OTHER_HOSPITAL_ID, auth))
                .isInstanceOf(AccessDeniedException.class);
        verify(notificationRepository, never()).findByHospitalIdOrderBySentAtDesc(any());
    }

    @Test
    void listByHospital_adminBypassesOwnershipCheck_evenWithNoHospitalClaim() {
        Authentication auth = authWithHospital(null, "ADMIN");
        when(notificationRepository.findByHospitalIdOrderBySentAtDesc(OTHER_HOSPITAL_ID)).thenReturn(List.of());

        service.listByHospital(OTHER_HOSPITAL_ID, auth);

        verify(notificationRepository).findByHospitalIdOrderBySentAtDesc(OTHER_HOSPITAL_ID);
    }

    // ── listByDonor ─────────────────────────────────────────────────────────

    @Test
    void listByDonor_ownDonorRecord_succeeds() {
        Authentication auth = authWithHospital(null, "DONOR");
        when(donorLookupPort.findDonorIdByEmail(CALLER_EMAIL)).thenReturn(Optional.of(OWN_DONOR_ID));
        when(notificationRepository.findByDonorIdOrderBySentAtDesc(OWN_DONOR_ID)).thenReturn(List.of());

        service.listByDonor(OWN_DONOR_ID, auth);

        verify(notificationRepository).findByDonorIdOrderBySentAtDesc(OWN_DONOR_ID);
    }

    @Test
    void listByDonor_someoneElsesDonorRecord_throwsAccessDenied() {
        Authentication auth = authWithHospital(null, "DONOR");
        when(donorLookupPort.findDonorIdByEmail(CALLER_EMAIL)).thenReturn(Optional.of(OWN_DONOR_ID));

        assertThatThrownBy(() -> service.listByDonor(OTHER_DONOR_ID, auth))
                .isInstanceOf(AccessDeniedException.class);
        verify(notificationRepository, never()).findByDonorIdOrderBySentAtDesc(any());
    }

    @Test
    void listByDonor_adminBypassesOwnershipCheck() {
        Authentication auth = authWithHospital(null, "ADMIN");
        when(notificationRepository.findByDonorIdOrderBySentAtDesc(OTHER_DONOR_ID)).thenReturn(List.of());

        service.listByDonor(OTHER_DONOR_ID, auth);

        verify(notificationRepository).findByDonorIdOrderBySentAtDesc(OTHER_DONOR_ID);
        verify(donorLookupPort, never()).findDonorIdByEmail(anyString());
    }

    // ── markRead ────────────────────────────────────────────────────────────

    @Test
    void markRead_notificationNotFound_throws() {
        when(notificationRepository.findById(1L)).thenReturn(Optional.empty());
        Authentication auth = authWithHospital(OWN_HOSPITAL_ID, "OFFICER");

        assertThatThrownBy(() -> service.markRead(1L, auth))
                .isInstanceOf(NotificationNotFoundException.class);
    }

    @Test
    void markRead_ownHospitalNotification_marksReadAndSaves() {
        Notification notification = hospitalNotification(1L, OWN_HOSPITAL_ID);
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(notification));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));
        Authentication auth = authWithHospital(OWN_HOSPITAL_ID, "OFFICER");

        service.markRead(1L, auth);

        assertThat(notification.isRead()).isTrue();
        verify(notificationRepository).save(notification);
    }

    @Test
    void markRead_someoneElsesHospitalNotification_throwsAndNeverSaves() {
        Notification notification = hospitalNotification(1L, OTHER_HOSPITAL_ID);
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(notification));
        Authentication auth = authWithHospital(OWN_HOSPITAL_ID, "OFFICER");

        assertThatThrownBy(() -> service.markRead(1L, auth)).isInstanceOf(AccessDeniedException.class);

        assertThat(notification.isRead()).isFalse();
        verify(notificationRepository, never()).save(any());
    }

    @Test
    void markRead_ownDonorNotification_succeeds() {
        Notification notification = donorNotification(2L, OWN_DONOR_ID);
        when(notificationRepository.findById(2L)).thenReturn(Optional.of(notification));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));
        when(donorLookupPort.findDonorIdByEmail(CALLER_EMAIL)).thenReturn(Optional.of(OWN_DONOR_ID));
        Authentication auth = authWithHospital(null, "DONOR");

        service.markRead(2L, auth);

        assertThat(notification.isRead()).isTrue();
    }

    @Test
    void markRead_adminCanMarkAnyNotificationRead() {
        Notification notification = hospitalNotification(1L, OTHER_HOSPITAL_ID);
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(notification));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));
        Authentication auth = authWithHospital(null, "ADMIN");

        service.markRead(1L, auth);

        assertThat(notification.isRead()).isTrue();
    }

    // ── markAllReadForHospital / markAllReadForDonor ───────────────────────

    @Test
    void markAllReadForHospital_ownHospital_delegatesToRepository() {
        Authentication auth = authWithHospital(OWN_HOSPITAL_ID, "OFFICER");

        service.markAllReadForHospital(OWN_HOSPITAL_ID, auth);

        verify(notificationRepository).markAllReadByHospital(OWN_HOSPITAL_ID);
    }

    @Test
    void markAllReadForHospital_someoneElsesHospital_throwsAndNeverCallsRepository() {
        Authentication auth = authWithHospital(OWN_HOSPITAL_ID, "OFFICER");

        assertThatThrownBy(() -> service.markAllReadForHospital(OTHER_HOSPITAL_ID, auth))
                .isInstanceOf(AccessDeniedException.class);
        verify(notificationRepository, never()).markAllReadByHospital(any());
    }

    @Test
    void markAllReadForDonor_ownDonor_delegatesToRepository() {
        Authentication auth = authWithHospital(null, "DONOR");
        when(donorLookupPort.findDonorIdByEmail(CALLER_EMAIL)).thenReturn(Optional.of(OWN_DONOR_ID));

        service.markAllReadForDonor(OWN_DONOR_ID, auth);

        verify(notificationRepository).markAllReadByDonor(OWN_DONOR_ID);
    }

    private Notification hospitalNotification(Long id, Long hospitalId) {
        return Notification.builder()
                .id(id)
                .hospitalId(hospitalId)
                .message("Transfer update")
                .sentAt(LocalDateTime.now())
                .type("TRANSFER")
                .build();
    }

    private Notification donorNotification(Long id, Long donorId) {
        return Notification.builder()
                .id(id)
                .donorId(donorId)
                .message("Welcome")
                .sentAt(LocalDateTime.now())
                .type("DONOR")
                .build();
    }
}
