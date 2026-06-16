package com.blood.inventory.service;

import com.blood.inventory.event.BloodExpiredEvent;
import com.blood.inventory.event.BloodExpiringEvent;
import com.blood.inventory.model.BloodInventory;
import com.blood.inventory.repository.BloodInventoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
class ExpiryMonitoringServiceImpl implements ExpiryMonitoringService {

    private final BloodInventoryRepository inventoryRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    /**
     * Runs daily at midnight UTC.
     * Marks expired units — sets unitsAvailable to 0 and logs them.
     * Even if this job is down overnight, on restart it processes the full backlog
     * (query finds all records where expiryDate < today AND unitsAvailable > 0).
     */
    @Override
    @Scheduled(cron = "0 0 0 * * *", zone = "UTC")
    @Transactional
    public void processExpiredStock() {
        LocalDate today = LocalDate.now(clock);
        List<BloodInventory> expired = inventoryRepository.findExpiredWithStock(today);

        for (BloodInventory inv : expired) {
            int units = inv.getUnitsAvailable();
            inv.setUnitsAvailable(0);
            inv.setLastUpdated(LocalDateTime.now(clock));
            inventoryRepository.save(inv);

            eventPublisher.publishEvent(new BloodExpiredEvent(
                    inv.getId(), inv.getHospitalId(), inv.getBloodGroup().getValue(),
                    units, inv.getExpiryDate(), Instant.now(clock)));

            log.warn("Expired stock zeroed: inventoryId={} hospitalId={} bloodGroup={} units={}",
                    inv.getId(), inv.getHospitalId(), inv.getBloodGroup().getValue(), units);
        }

        if (!expired.isEmpty()) {
            log.info("Expiry job completed: {} records processed", expired.size());
        }
    }

    /**
     * Runs daily at 08:00 UTC — officers get an early-morning notification.
     * Groups expiring items by hospital and publishes one event per hospital.
     */
    @Override
    @Scheduled(cron = "0 0 8 * * *", zone = "UTC")
    @Transactional(readOnly = true)
    public void notifyExpiringSoon() {
        LocalDate today = LocalDate.now(clock);
        LocalDate threshold = today.plusDays(7);
        List<BloodInventory> expiring = inventoryRepository.findExpiringSoon(today, threshold);

        if (expiring.isEmpty()) return;

        Map<Long, List<BloodInventory>> byHospital = expiring.stream()
                .collect(Collectors.groupingBy(BloodInventory::getHospitalId));

        byHospital.forEach((hospitalId, items) -> {
            List<BloodExpiringEvent.ExpiringItem> eventItems = items.stream()
                    .map(inv -> new BloodExpiringEvent.ExpiringItem(
                            inv.getId(),
                            inv.getBloodGroup().getValue(),
                            inv.getUnitsAvailable(),
                            inv.getExpiryDate().toString()))
                    .toList();

            eventPublisher.publishEvent(new BloodExpiringEvent(hospitalId, eventItems, Instant.now(clock)));
            log.info("Expiring-soon alert published: hospitalId={} items={}", hospitalId, items.size());
        });
    }
}
