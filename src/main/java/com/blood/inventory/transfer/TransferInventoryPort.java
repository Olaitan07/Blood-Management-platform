package com.blood.inventory.transfer;

/**
 * Port exposed by the inventory module for atomic reservation/release/deduction
 * operations driven by the transfer workflow.
 */
public interface TransferInventoryPort {

    /**
     * Finds the best-available inventory slot (earliest expiry, enough net units)
     * and atomically reserves {@code quantity} units.
     *
     * @throws IllegalStateException if available units < quantity
     */
    InventorySlotDto findAndReserve(Long hospitalId, String bloodGroup, int quantity, String actor);

    /**
     * Releases a previously reserved quantity back to available.
     * Used on rejection, cancellation, or saga compensation (48 h expiry).
     */
    void release(Long inventoryId, int quantity, String actor);

    /**
     * Finalises a completed transfer. The full {@code approvedUnits} always
     * leaves the source (that's what was physically shipped, regardless of
     * what arrives), but only {@code receivedUnits} is credited to the
     * destination — any shortfall (breakage, expiry in transit) is a real
     * loss, not silently moved to either hospital's available stock.
     * The expiry date is read directly from the source inventory record.
     *
     * @return the new (or updated) destination inventory ID, or null if
     *         receivedUnits is 0 (nothing to credit at the destination)
     */
    Long finalizeTransfer(Long sourceInventoryId, int approvedUnits, int receivedUnits,
                          Long destHospitalId, String actor);
}
