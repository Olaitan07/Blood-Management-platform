package com.blood.inventory.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "blood_inventory")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BloodInventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "hospital_id", nullable = false)
    private Long hospitalId;

    @Column(name = "blood_group", nullable = false, length = 5)
    private BloodGroup bloodGroup;

    @Column(name = "units_available", nullable = false)
    private int unitsAvailable;

    @Column(name = "units_reserved", nullable = false)
    @Builder.Default
    private int unitsReserved = 0;

    @Column(name = "expiry_date", nullable = false)
    private LocalDate expiryDate;

    @Column(name = "last_updated", nullable = false)
    private LocalDateTime lastUpdated;

    // Optimistic locking — prevents lost updates under concurrent stock additions
    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    public InventoryStatus computeStatus(LocalDate today) {
        if (!expiryDate.isAfter(today)) return InventoryStatus.EXPIRED;
        if (expiryDate.isBefore(today.plusDays(8))) return InventoryStatus.EXPIRING_SOON;
        return InventoryStatus.AVAILABLE;
    }
}
