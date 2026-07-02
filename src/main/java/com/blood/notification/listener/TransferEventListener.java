package com.blood.notification.listener;

import com.blood.notification.model.Notification;
import com.blood.notification.model.NotificationStatus;
import com.blood.notification.model.ProcessedEvent;
import com.blood.notification.repository.NotificationRepository;
import com.blood.notification.repository.ProcessedEventRepository;
import com.blood.transfer.events.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class TransferEventListener {

    private final NotificationRepository notificationRepository;
    private final ProcessedEventRepository processedEventRepository;

    @ApplicationModuleListener
    public void onTransferRequested(BloodTransferRequestedEvent event) {
        String key = "transfer-requested:" + event.transferId();
        if (isDuplicate(key)) return;

        String message = String.format(
                "New blood transfer request #%d: %d units of %s requested from your hospital by hospital #%d",
                event.transferId(), event.quantity(), event.bloodGroup(), event.requestingHospitalId());

        saveNotification(event.transferId(), event.sourceHospitalId(), message, key);
        log.info("Notification saved (PENDING) for transfer request: id={}", event.transferId());
    }

    @ApplicationModuleListener
    public void onTransferApproved(BloodTransferApprovedEvent event) {
        String key = "transfer-approved:" + event.transferId();
        if (isDuplicate(key)) return;

        String message = String.format(
                "Transfer request #%d approved: %d units of %s are being dispatched from hospital #%d",
                event.transferId(), event.quantity(), event.bloodGroup(), event.sourceHospitalId());

        saveNotification(event.transferId(), event.requestingHospitalId(), message, key);
        log.info("Notification saved (PENDING) for transfer approval: id={}", event.transferId());
    }

    @ApplicationModuleListener
    public void onTransferRejected(BloodTransferRejectedEvent event) {
        String key = "transfer-rejected:" + event.transferId();
        if (isDuplicate(key)) return;

        String message = String.format(
                "Transfer request #%d rejected by hospital #%d. Reason: %s. Please try another hospital.",
                event.transferId(), event.sourceHospitalId(), event.reason());

        saveNotification(event.transferId(), event.requestingHospitalId(), message, key);
        log.info("Notification saved (PENDING) for transfer rejection: id={}", event.transferId());
    }

    @ApplicationModuleListener
    public void onTransferCompleted(BloodTransferCompletedEvent event) {
        String key = "transfer-completed:" + event.transferId();
        if (isDuplicate(key)) return;

        String message = String.format(
                "Transfer #%d completed: %d of %d units of %s received from hospital #%d",
                event.transferId(), event.quantityReceived(), event.quantityApproved(),
                event.bloodGroup(), event.sourceHospitalId());

        saveNotification(event.transferId(), event.requestingHospitalId(), message, key);
        log.info("Notification saved (PENDING) for transfer completion: id={}", event.transferId());
    }

    @ApplicationModuleListener
    public void onTransferCancelled(BloodTransferCancelledEvent event) {
        String key = "transfer-cancelled:" + event.transferId();
        if (isDuplicate(key)) return;

        String message = event.wasApproved()
                ? String.format("Transfer #%d has been cancelled and the %d units of %s reservation has been released",
                        event.transferId(), event.quantity(), event.bloodGroup())
                : String.format("Transfer request #%d for %d units of %s has been cancelled",
                        event.transferId(), event.quantity(), event.bloodGroup());

        // Notify both hospitals
        saveNotification(event.transferId(), event.requestingHospitalId(), message, key);
        saveNotification(event.transferId(), event.sourceHospitalId(), message, key + ":src");

        processedEventRepository.save(ProcessedEvent.builder()
                .eventKey(key + ":src")
                .processedAt(Instant.now())
                .build());

        log.info("Notification saved (PENDING) for transfer cancellation: id={}", event.transferId());
    }

    private boolean isDuplicate(String key) {
        if (processedEventRepository.existsById(key)) {
            log.warn("Duplicate transfer event skipped: {}", key);
            return true;
        }
        return false;
    }

    private void saveNotification(Long transferId, Long hospitalId, String message, String eventKey) {
        notificationRepository.save(Notification.builder()
                .transferId(transferId)
                .hospitalId(hospitalId)
                .recipient("hospital:" + hospitalId)
                .message(message)
                .sentAt(LocalDateTime.now())
                .type("TRANSFER")
                .status(NotificationStatus.PENDING)
                .build());

        processedEventRepository.save(ProcessedEvent.builder()
                .eventKey(eventKey)
                .processedAt(Instant.now())
                .build());
    }
}
