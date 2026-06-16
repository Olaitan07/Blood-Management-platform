package com.blood.inventory.service;

import com.blood.inventory.dto.AddInventoryRequest;
import com.blood.inventory.dto.AuditLogResponse;
import com.blood.inventory.dto.InventoryResponse;
import com.blood.inventory.dto.UpdateInventoryRequest;
import org.springframework.security.core.Authentication;

import java.util.List;

public interface InventoryService {

    InventoryResponse addStock(AddInventoryRequest request, Authentication auth);

    InventoryResponse updateStock(Long inventoryId, UpdateInventoryRequest request, Authentication auth);

    List<InventoryResponse> getHospitalInventory(Authentication auth);

    List<AuditLogResponse> getAuditLog(Long inventoryId, Authentication auth);
}
