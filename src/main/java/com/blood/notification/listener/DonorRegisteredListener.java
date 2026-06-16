package com.blood.notification.listener;

import com.blood.donor.event.DonorRegisteredEvent;
import com.blood.notification.model.Notification;
import com.blood.notification.model.NotificationStatus;
import com.blood.notification.model.ProcessedEvent;
import com.blood.notification.repository.NotificationRepository;
import com.blood.notification.repository.ProcessedEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class DonorRegisteredListener {

    private final NotificationRepository notificationRepository;
    private final ProcessedEventRepository processedEventRepository;

    /**
     * Idempotency: keyed on donorId — re-delivery (at-least-once) stores only ONE notification.
     * Spring Modulith's event_publication outbox ensures this listener is called even after restart,
     * so no donor welcome message is ever lost.
     * Privacy boundary: only data present in the event is used — phone/address excluded intentionally.
     */
    @ApplicationModuleListener
    public void onDonorRegistered(DonorRegisteredEvent event) {
        String eventKey = "donor-registered:" + event.donorId();

        if (processedEventRepository.existsById(eventKey)) {
            log.warn("Duplicate donor-registered event skipped: donorId={}", event.donorId());
            return;
        }

        String message = String.format(
                "Welcome %s! Your donor registration is confirmed. Blood group: %s",
                event.fullName(), event.bloodGroup());

        notificationRepository.save(Notification.builder()
                .donorId(event.donorId())
                .recipient("donor:" + event.donorId())
                .message(message)
                .sentAt(LocalDateTime.now())
                .type("DONOR")
                .status(NotificationStatus.PENDING)
                .build());

        processedEventRepository.save(ProcessedEvent.builder()
                .eventKey(eventKey)
                .processedAt(Instant.now())
                .build());

        log.info("Notification saved (PENDING) for donorId={}", event.donorId());
    }
}
