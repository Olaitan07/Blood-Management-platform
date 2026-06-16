package com.blood.inventory.service;

public interface ExpiryMonitoringService {

    /** Marks expired units and publishes BloodExpiredEvent per record. Run daily. */
    void processExpiredStock();

    /** Finds stock expiring within 7 days and publishes BloodExpiringEvent per hospital. Run daily. */
    void notifyExpiringSoon();
}
