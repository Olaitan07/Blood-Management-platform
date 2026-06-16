package com.blood.inventory.service;

import com.blood.inventory.dto.AddInventoryRequest;
import com.blood.inventory.dto.AuditLogResponse;
import com.blood.inventory.dto.InventoryResponse;
import com.blood.inventory.dto.UpdateInventoryRequest;
import com.blood.inventory.event.BloodAddedEvent;
import com.blood.inventory.event.BloodUpdatedEvent;
import com.blood.inventory.exception.InsufficientStockException;
import com.blood.inventory.exception.InventoryNotFoundException;
import com.blood.inventory.model.BloodInventory;
import com.blood.inventory.model.InventoryAuditLog;
import com.blood.inventory.repository.BloodInventoryRepository;
import com.blood.inventory.repository.InventoryAuditLogRepository;
import com.blood.inventory.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
class InventoryServiceImpl implements InventoryService {

    private static final int WHOLE_BLOOD_MAX_SHELF_DAYS = 42;

    private final BloodInventoryRepository inventoryRepository;
    private final InventoryAuditLogRepository auditLogRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    @Override
    @Transactional
    public InventoryResponse addStock(AddInventoryRequest request, Authentication auth) {
        Long hospitalId = SecurityUtils.extractHospitalId(auth);
        LocalDate today = LocalDate.now(clock);

        if (request.expiryDate().isBefore(today) || request.expiryDate().isEqual(today)) {
            throw new IllegalArgumentException("Expiry date must be in the future — expired blood cannot enter inventory");
        }

        long daysUntilExpiry = ChronoUnit.DAYS.between(today, request.expiryDate());
        if (daysUntilExpiry > WHOLE_BLOOD_MAX_SHELF_DAYS && !request.confirmShelfLife()) {
            // Return a warning — the client must resubmit with confirmShelfLife=true
            throw new IllegalStateException(
                    "Expiry date is more than " + WHOLE_BLOOD_MAX_SHELF_DAYS + " days away — unusual for whole blood. " +
                    "Resubmit with confirmShelfLife=true if this is intentional.");
        }

        BloodInventory inventory = BloodInventory.builder()
                .hospitalId(hospitalId)
                .bloodGroup(request.bloodGroup())
                .unitsAvailable(request.units())
                .unitsReserved(0)
                .expiryDate(request.expiryDate())
                .lastUpdated(LocalDateTime.now(clock))
                .build();

        // @Version is set automatically on first save
        BloodInventory saved = inventoryRepository.save(inventory);

        auditLogRepository.save(InventoryAuditLog.builder()
                .inventoryId(saved.getId())
                .hospitalId(hospitalId)
                .bloodGroup(saved.getBloodGroup().getValue())
                .oldUnits(0)
                .newUnits(saved.getUnitsAvailable())
                .reason("Initial stock addition")
                .changedBy(auth.getName())
                .changedAt(LocalDateTime.now(clock))
                .build());

        eventPublisher.publishEvent(new BloodAddedEvent(
                saved.getId(), hospitalId, saved.getBloodGroup().getValue(),
                saved.getUnitsAvailable(), saved.getExpiryDate(), Instant.now(clock)));

        log.info("Stock added: inventoryId={} hospitalId={} bloodGroup={} units={}",
                saved.getId(), hospitalId, saved.getBloodGroup().getValue(), saved.getUnitsAvailable());

        boolean warning = daysUntilExpiry > WHOLE_BLOOD_MAX_SHELF_DAYS;
        return warning ? InventoryResponse.withWarning(saved, today) : InventoryResponse.from(saved, today);
    }

    @Override
    @Transactional
    public InventoryResponse updateStock(Long inventoryId, UpdateInventoryRequest request, Authentication auth) {
        Long hospitalId = SecurityUtils.extractHospitalId(auth);
        LocalDate today = LocalDate.now(clock);

        BloodInventory inventory = inventoryRepository.findByIdAndHospitalId(inventoryId, hospitalId)
                .orElseThrow(() -> new InventoryNotFoundException(inventoryId));

        int newUnits = request.units();
        if (newUnits < 0) {
            throw new IllegalArgumentException("Units cannot be negative. Current stock: " + inventory.getUnitsAvailable());
        }

        // Cannot reduce below reserved units
        if (newUnits < inventory.getUnitsReserved()) {
            throw new InsufficientStockException(newUnits, inventory.getUnitsReserved());
        }

        int oldUnits = inventory.getUnitsAvailable();
        inventory.setUnitsAvailable(newUnits);
        inventory.setLastUpdated(LocalDateTime.now(clock));

        BloodInventory saved = inventoryRepository.save(inventory);

        auditLogRepository.save(InventoryAuditLog.builder()
                .inventoryId(saved.getId())
                .hospitalId(hospitalId)
                .bloodGroup(saved.getBloodGroup().getValue())
                .oldUnits(oldUnits)
                .newUnits(newUnits)
                .reason(request.reason())
                .changedBy(auth.getName())
                .changedAt(LocalDateTime.now(clock))
                .build());

        eventPublisher.publishEvent(new BloodUpdatedEvent(
                saved.getId(), hospitalId, saved.getBloodGroup().getValue(),
                oldUnits, newUnits, request.reason(), auth.getName(), Instant.now(clock)));

        log.info("Stock updated: inventoryId={} {} → {} by {}", saved.getId(), oldUnits, newUnits, auth.getName());
        return InventoryResponse.from(saved, today);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryResponse> getHospitalInventory(Authentication auth) {
        Long hospitalId = SecurityUtils.extractHospitalId(auth);
        LocalDate today = LocalDate.now(clock);
        // Query filters expired records at DB level — no stale data even if job hasn't run
        return inventoryRepository.findAvailableByHospital(hospitalId, today)
                .stream()
                .map(inv -> InventoryResponse.from(inv, today))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditLogResponse> getAuditLog(Long inventoryId, Authentication auth) {
        Long hospitalId = SecurityUtils.extractHospitalId(auth);
        // Validate ownership
        inventoryRepository.findByIdAndHospitalId(inventoryId, hospitalId)
                .orElseThrow(() -> new InventoryNotFoundException(inventoryId));
        return auditLogRepository.findByInventoryIdOrderByChangedAtDesc(inventoryId)
                .stream()
                .map(AuditLogResponse::from)
                .toList();
    }
}
