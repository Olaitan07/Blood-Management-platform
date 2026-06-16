package com.blood.inventory.search;

import com.blood.inventory.model.BloodGroup;

import java.time.LocalDate;
import java.util.List;

/**
 * Port exposed by the inventory module for cross-module blood-availability queries.
 * Only accessible through the inventory::search named interface.
 */
public interface BloodSearchPort {

    /**
     * Returns all non-expired inventory records for the given blood group
     * that belong to one of the supplied active hospital IDs and have at
     * least one unit available after subtracting reserved stock.
     *
     * @throws IllegalArgumentException if bloodGroup is not a valid value
     */
    List<AvailableBloodDto> findAvailable(String bloodGroup, List<Long> activeHospitalIds, LocalDate today);

    /** The list of accepted blood group strings, e.g. ["A+", "A-", ...] */
    static List<String> validBloodGroups() {
        return BloodGroup.VALID_VALUES;
    }
}
