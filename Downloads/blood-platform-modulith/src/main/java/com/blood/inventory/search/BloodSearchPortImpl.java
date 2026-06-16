package com.blood.inventory.search;

import com.blood.inventory.model.BloodGroup;
import com.blood.inventory.repository.BloodInventoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
class BloodSearchPortImpl implements BloodSearchPort {

    private final BloodInventoryRepository inventoryRepository;
    private final Clock clock;

    @Override
    @Transactional(readOnly = true)
    public List<AvailableBloodDto> findAvailable(String bloodGroup, List<Long> activeHospitalIds, LocalDate today) {
        if (activeHospitalIds.isEmpty()) {
            return List.of();
        }
        BloodGroup bg = BloodGroup.fromValue(bloodGroup);
        return inventoryRepository
                .findAvailableByBloodGroupAcrossHospitals(bg, activeHospitalIds, today)
                .stream()
                .map(inv -> new AvailableBloodDto(
                        inv.getHospitalId(),
                        inv.getBloodGroup().getValue(),
                        inv.getUnitsAvailable() - inv.getUnitsReserved(),
                        inv.getLastUpdated()))
                .toList();
    }
}
