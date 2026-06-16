package com.blood.inventory.reporting;

import com.blood.hospital.service.HospitalService;
import com.blood.inventory.model.BloodInventory;
import com.blood.inventory.repository.BloodInventoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
class InventoryReportPortImpl implements InventoryReportPort {

    private final BloodInventoryRepository inventoryRepository;
    private final HospitalService hospitalService;

    @Override
    @Transactional(readOnly = true)
    public List<StockLevelDto> currentStockLevels() {
        Map<Long, String> hospitalNames = hospitalService.listAllHospitals().stream()
                .collect(Collectors.toMap(h -> h.id(), h -> h.name()));

        return inventoryRepository.findAll().stream()
                .map(i -> new StockLevelDto(
                        i.getHospitalId(),
                        hospitalNames.getOrDefault(i.getHospitalId(), "Hospital #" + i.getHospitalId()),
                        i.getBloodGroup().getValue(),
                        i.getUnitsAvailable(),
                        i.getUnitsReserved(),
                        i.getUnitsAvailable() - i.getUnitsReserved()))
                .sorted((a, b) -> {
                    int cmp = Long.compare(a.hospitalId(), b.hospitalId());
                    return cmp != 0 ? cmp : a.bloodGroup().compareTo(b.bloodGroup());
                })
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExpiryWasteDto> expiryWaste(LocalDate from, LocalDate to) {
        Map<Long, String> hospitalNames = hospitalService.listAllHospitals().stream()
                .collect(Collectors.toMap(h -> h.id(), h -> h.name()));

        List<BloodInventory> wasted = inventoryRepository.findWastedInRange(from, to);

        return wasted.stream()
                .map(i -> new ExpiryWasteDto(
                        i.getHospitalId(),
                        hospitalNames.getOrDefault(i.getHospitalId(), "Hospital #" + i.getHospitalId()),
                        i.getBloodGroup().getValue(),
                        i.getUnitsAvailable(),
                        i.getExpiryDate().toString()))
                .toList();
    }
}
