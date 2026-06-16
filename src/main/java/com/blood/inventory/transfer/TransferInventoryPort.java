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
     * Finalises a completed transfer:
     * deducts {@code units} from source inventory and adds them to the destination.
     * The expiry date is read directly from the source inventory record.
     *
     * @return the new (or updated) destination inventory ID
     */
    Long finalizeTransfer(Long sourceInventoryId, int units,
                          Long destHospitalId, String actor);
}
