package com.blood.inventory.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class BloodInventoryTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 7, 3);

    private BloodInventory inventoryExpiring(LocalDate expiryDate) {
        return BloodInventory.builder()
                .id(1L)
                .hospitalId(10L)
                .bloodGroup(BloodGroup.O_POSITIVE)
                .unitsAvailable(5)
                .unitsReserved(0)
                .expiryDate(expiryDate)
                .lastUpdated(LocalDateTime.of(2026, 7, 1, 0, 0))
                .build();
    }

    @Test
    void computeStatus_expiryIsToday_isExpired() {
        assertThat(inventoryExpiring(TODAY).computeStatus(TODAY)).isEqualTo(InventoryStatus.EXPIRED);
    }

    @Test
    void computeStatus_expiryWasYesterday_isExpired() {
        assertThat(inventoryExpiring(TODAY.minusDays(1)).computeStatus(TODAY)).isEqualTo(InventoryStatus.EXPIRED);
    }

    @Test
    void computeStatus_expiryInSevenDays_isExpiringSoon() {
        assertThat(inventoryExpiring(TODAY.plusDays(7)).computeStatus(TODAY)).isEqualTo(InventoryStatus.EXPIRING_SOON);
    }

    @Test
    void computeStatus_expiryInEightDays_isAvailable() {
        assertThat(inventoryExpiring(TODAY.plusDays(8)).computeStatus(TODAY)).isEqualTo(InventoryStatus.AVAILABLE);
    }

    @Test
    void computeStatus_expiryInThirtyDays_isAvailable() {
        assertThat(inventoryExpiring(TODAY.plusDays(30)).computeStatus(TODAY)).isEqualTo(InventoryStatus.AVAILABLE);
    }
}
