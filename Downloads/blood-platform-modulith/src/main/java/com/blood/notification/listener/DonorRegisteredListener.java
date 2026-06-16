package com.blood.notification.listener;

import com.blood.donor.event.DonorRegisteredEvent;
import com.blood.notification.model.Notification;
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
     * Idempotency: keyed on donorId so re-delivery (at-least-once) sends only ONE welcome SMS.
     * Spring Modulith's @ApplicationModuleListener runs in a transaction, so the processed_events
     * insert and the notification insert are atomic.
     */
    @ApplicationModuleListener
    public void onDonorRegistered(DonorRegisteredEvent event) {
        String eventKey = "donor-registered:" + event.donorId();

        if (processedEventRepository.existsById(eventKey)) {
            log.warn("Duplicate event skipped: {}", eventKey);
            return;
        }

        String message = String.format(
                "Welcome %s! Your donor registration is confirmed. Blood group: %s",
                event.fullName(), event.bloodGroup());

        Notification notification = Notification.builder()
                .donorId(event.donorId())
                .message(message)
                .sentAt(LocalDateTime.now())
                .build();

        notificationRepository.save(notification);

        processedEventRepository.save(ProcessedEvent.builder()
                .eventKey(eventKey)
                .processedAt(Instant.now())
                .build());

        log.info("Welcome SMS queued for donorId={}", event.donorId());
    }
}
