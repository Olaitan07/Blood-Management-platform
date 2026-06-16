package com.blood.transfer.scheduler;

import com.blood.inventory.transfer.TransferInventoryPort;
import com.blood.transfer.events.BloodTransferCancelledEvent;
import com.blood.transfer.model.BloodTransferRequest;
import com.blood.transfer.model.TransferStatus;
import com.blood.transfer.repository.BloodTransferRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Saga compensation: APPROVED transfers not completed within 48 hours
 * are automatically expired and their inventory reservation released.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TransferExpiryScheduler {

    private static final int APPROVAL_TIMEOUT_HOURS = 48;

    private final BloodTransferRepository transferRepository;
    private final TransferInventoryPort inventoryPort;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    @Scheduled(fixedDelay = 15 * 60 * 1000) // every 15 minutes
    @Transactional
    public void expireStaleApprovedTransfers() {
        LocalDateTime cutoff = LocalDateTime.now(clock).minusHours(APPROVAL_TIMEOUT_HOURS);
        List<BloodTransferRequest> stale = transferRepository.findApprovedOlderThan(cutoff);

        if (stale.isEmpty()) return;

        log.info("Expiry job: found {} stale APPROVED transfers", stale.size());

        for (BloodTransferRequest transfer : stale) {
            try {
                transfer.transitionTo(TransferStatus.EXPIRED);

                if (transfer.getSourceInventoryId() != null) {
                    inventoryPort.release(transfer.getSourceInventoryId(),
                            transfer.getQuantity(), "system [48h-expiry]");
                }

                transferRepository.save(transfer);

                eventPublisher.publishEvent(new BloodTransferCancelledEvent(
                        transfer.getId(), transfer.getRequestingHospitalId(), transfer.getSourceHospitalId(),
                        transfer.getBloodGroup(), transfer.getQuantity(), true,
                        "system [48h-expiry]", Instant.now(clock)));

                log.info("Transfer expired: id={} reservation released", transfer.getId());
            } catch (Exception e) {
                log.error("Failed to expire transferId={}: {}", transfer.getId(), e.getMessage());
            }
        }
    }
}
