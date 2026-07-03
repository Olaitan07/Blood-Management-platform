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
    public Long finalizeTransfer(Long sourceInventoryId, int approvedUnits, int receivedUnits,
                                 Long destHospitalId, String actor) {
        BloodInventory source = inventoryRepository.findById(sourceInventoryId)
                .orElseThrow(() -> new IllegalStateException("Source inventory not found: " + sourceInventoryId));

        LocalDate expiryDate = source.getExpiryDate();
        String bloodGroup = source.getBloodGroup().getValue();
        int shortfall = approvedUnits - receivedUnits;

        // The full approved amount always leaves the source — that's what was
        // physically shipped, regardless of what arrives intact.
        source.setUnitsAvailable(source.getUnitsAvailable() - approvedUnits);
        source.setUnitsReserved(Math.max(0, source.getUnitsReserved() - approvedUnits));
        source.setLastUpdated(LocalDateTime.now(clock));
        inventoryRepository.save(source);

        String dispatchReason = shortfall > 0
                ? "Transfer dispatched: " + approvedUnits + " units sent to hospitalId=" + destHospitalId +
                  " (" + shortfall + " lost/expired in transit, not credited on arrival)"
                : "Transfer dispatched: " + approvedUnits + " units sent to hospitalId=" + destHospitalId;
        auditLogRepository.save(InventoryAuditLog.builder()
                .inventoryId(sourceInventoryId)
                .hospitalId(source.getHospitalId())
                .bloodGroup(bloodGroup)
                .oldUnits(source.getUnitsAvailable() + approvedUnits)
                .newUnits(source.getUnitsAvailable())
                .reason(dispatchReason)
                .changedBy(actor)
                .changedAt(LocalDateTime.now(clock))
                .build());

        if (receivedUnits <= 0) {
            log.info("Transfer finalised: {}x{} shipped from hospitalId={}, 0 credited to hospitalId={} (total loss in transit)",
                    approvedUnits, bloodGroup, source.getHospitalId(), destHospitalId);
            return null;
        }

        // Only what was actually confirmed received is credited to the destination.
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
                    .unitsAvailable(receivedUnits)
                    .unitsReserved(0)
                    .expiryDate(expiryDate)
                    .lastUpdated(LocalDateTime.now(clock))
                    .build();
        } else {
            dest.setUnitsAvailable(dest.getUnitsAvailable() + receivedUnits);
            dest.setLastUpdated(LocalDateTime.now(clock));
        }

        BloodInventory savedDest = inventoryRepository.save(dest);

        String receivedReason = shortfall > 0
                ? "Transfer received: " + receivedUnits + " of " + approvedUnits + " units from hospitalId=" +
                  source.getHospitalId() + " (" + shortfall + " lost/expired in transit)"
                : "Transfer received: " + receivedUnits + " units from hospitalId=" + source.getHospitalId();
        auditLogRepository.save(InventoryAuditLog.builder()
                .inventoryId(savedDest.getId())
                .hospitalId(destHospitalId)
                .bloodGroup(bloodGroup)
                .oldUnits(savedDest.getUnitsAvailable() - receivedUnits)
                .newUnits(savedDest.getUnitsAvailable())
                .reason(receivedReason)
                .changedBy(actor)
                .changedAt(LocalDateTime.now(clock))
                .build());

        log.info("Transfer finalised: {}x{} shipped from hospitalId={}, {} credited to hospitalId={}",
                approvedUnits, bloodGroup, source.getHospitalId(), receivedUnits, destHospitalId);
        return savedDest.getId();
    }
}
