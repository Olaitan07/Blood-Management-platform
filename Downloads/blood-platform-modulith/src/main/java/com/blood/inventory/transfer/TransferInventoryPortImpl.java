package com.blood.inventory.transfer;

import com.blood.inventory.model.BloodGroup;
import com.blood.inventory.model.BloodInventory;
import com.blood.inventory.model.InventoryAuditLog;
import com.blood.inventory.repository.BloodInventoryRepository;
import com.blood.inventory.repository.InventoryAuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
class TransferInventoryPortImpl implements TransferInventoryPort {

    private final BloodInventoryRepository inventoryRepository;
    private final InventoryAuditLogRepository auditLogRepository;
    private final Clock clock;

    @Override
    @Transactional
    public InventorySlotDto findAndReserve(Long hospitalId, String bloodGroup, int quantity, String actor) {
        BloodGroup bg = BloodGroup.fromValue(bloodGroup);
        LocalDate today = LocalDate.now(clock);

        BloodInventory slot = inventoryRepository
                .findAvailableByHospital(hospitalId, today)
                .stream()
                .filter(i -> i.getBloodGroup() == bg)
                .filter(i -> (i.getUnitsAvailable() - i.getUnitsReserved()) >= quantity)
                .min((a, b) -> a.getExpiryDate().compareTo(b.getExpiryDate()))
                .orElseThrow(() -> new IllegalStateException(
                        "Insufficient stock: " + bloodGroup + " at hospitalId=" + hospitalId +
                        ". Available units may have dropped since search."));

        int available = slot.getUnitsAvailable() - slot.getUnitsReserved();
        slot.setUnitsReserved(slot.getUnitsReserved() + quantity);
        slot.setLastUpdated(LocalDateTime.now(clock));
        inventoryRepository.save(slot);

        auditLogRepository.save(InventoryAuditLog.builder()
                .inventoryId(slot.getId())
                .hospitalId(hospitalId)
                .bloodGroup(bloodGroup)
                .oldUnits(slot.getUnitsAvailable())
                .newUnits(slot.getUnitsAvailable())
                .reason("Transfer reservation: " + quantity + " units reserved for transfer by " + actor)
                .changedBy(actor)
                .changedAt(LocalDateTime.now(clock))
                .build());

        log.info("Reserved {}x{} from inventoryId={} for transfer by {}", quantity, bloodGroup, slot.getId(), actor);
        return new InventorySlotDto(slot.getId(), available, slot.getExpiryDate());
    }

    @Override
    @Transactional
    public void release(Long inventoryId, int quantity, String actor) {
        BloodInventory inv = inventoryRepository.findById(inventoryId)
                .orElseThrow(() -> new IllegalStateException("Inventory record not found: " + inventoryId));

        int released = Math.min(quantity, inv.getUnitsReserved());
        inv.setUnitsReserved(inv.getUnitsReserved() - released);
        inv.setLastUpdated(LocalDateTime.now(clock));
        inventoryRepository.save(inv);

        auditLogRepository.save(InventoryAuditLog.builder()
                .inventoryId(inventoryId)
                .hospitalId(inv.getHospitalId())
                .bloodGroup(inv.getBloodGroup().getValue())
                .oldUnits(inv.getUnitsAvailable())
                .newUnits(inv.getUnitsAvailable())
                .reason("Transfer reservation released: " + released + " units by " + actor)
                .changedBy(actor)
                .changedAt(LocalDateTime.now(clock))
                .build());

        log.info("Released {}x{} reservation from inventoryId={} by {}", released, inv.getBloodGroup().getValue(), inventoryId, actor);
    }

    @Override
    @Transactional
    public Long finalizeTransfer(Long sourceInventoryId, int units,
                                 Long destHospitalId, String actor) {
        BloodInventory source = inventoryRepository.findById(sourceInventoryId)
                .orElseThrow(() -> new IllegalStateException("Source inventory not found: " + sourceInventoryId));

        LocalDate expiryDate = source.getExpiryDate();
        String bloodGroup = source.getBloodGroup().getValue();

        // Deduct from source: reduce both available and reserved
        source.setUnitsAvailable(source.getUnitsAvailable() - units);
        source.setUnitsReserved(Math.max(0, source.getUnitsReserved() - units));
        source.setLastUpdated(LocalDateTime.now(clock));
        inventoryRepository.save(source);

        auditLogRepository.save(InventoryAuditLog.builder()
                .inventoryId(sourceInventoryId)
                .hospitalId(source.getHospitalId())
                .bloodGroup(bloodGroup)
                .oldUnits(source.getUnitsAvailable() + units)
                .newUnits(source.getUnitsAvailable())
                .reason("Transfer dispatched: " + units + " units sent to hospitalId=" + destHospitalId)
                .changedBy(actor)
                .changedAt(LocalDateTime.now(clock))
                .build());

        // Add to destination: find existing record for same group+expiry or create new
        BloodInventory dest = inventoryRepository
                .findAvailableByHospital(destHospitalId, LocalDate.now(clock))
                .stream()
                .filter(i -> i.getBloodGroup() == source.getBloodGroup())
                .filter(i -> i.getExpiryDate().isEqual(expiryDate))
                .findFirst()
                .orElse(null);

        if (dest == null) {
            dest = BloodInventory.builder()
                    .hospitalId(destHospitalId)
                    .bloodGroup(source.getBloodGroup())
                    .unitsAvailable(units)
                    .unitsReserved(0)
                    .expiryDate(expiryDate)
                    .lastUpdated(LocalDateTime.now(clock))
                    .build();
        } else {
            dest.setUnitsAvailable(dest.getUnitsAvailable() + units);
            dest.setLastUpdated(LocalDateTime.now(clock));
        }

        BloodInventory savedDest = inventoryRepository.save(dest);

        auditLogRepository.save(InventoryAuditLog.builder()
                .inventoryId(savedDest.getId())
                .hospitalId(destHospitalId)
                .bloodGroup(bloodGroup)
                .oldUnits(savedDest.getUnitsAvailable() - units)
                .newUnits(savedDest.getUnitsAvailable())
                .reason("Transfer received: " + units + " units from hospitalId=" + source.getHospitalId())
                .changedBy(actor)
                .changedAt(LocalDateTime.now(clock))
                .build());

        log.info("Transfer finalised: {}x{} from hospitalId={} to hospitalId={}", units, bloodGroup, source.getHospitalId(), destHospitalId);
        return savedDest.getId();
    }
}
