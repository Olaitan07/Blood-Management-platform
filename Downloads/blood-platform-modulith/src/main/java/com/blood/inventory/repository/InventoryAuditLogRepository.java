package com.blood.inventory.repository;

import com.blood.inventory.model.InventoryAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InventoryAuditLogRepository extends JpaRepository<InventoryAuditLog, Long> {

    List<InventoryAuditLog> findByInventoryIdOrderByChangedAtDesc(Long inventoryId);

    List<InventoryAuditLog> findByHospitalIdOrderByChangedAtDesc(Long hospitalId);
}
