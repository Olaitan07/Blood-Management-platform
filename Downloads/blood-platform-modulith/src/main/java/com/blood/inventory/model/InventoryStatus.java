package com.blood.inventory.model;

public enum InventoryStatus {
    AVAILABLE,
    EXPIRING_SOON,  // within 7 days
    EXPIRED
}
