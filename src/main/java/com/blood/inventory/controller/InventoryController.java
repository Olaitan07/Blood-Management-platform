package com.blood.inventory.controller;

import com.blood.inventory.dto.AddInventoryRequest;
import com.blood.inventory.dto.AuditLogResponse;
import com.blood.inventory.dto.ApiResponse;
import com.blood.inventory.dto.InventoryResponse;
import com.blood.inventory.dto.UpdateInventoryRequest;
import com.blood.inventory.service.InventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    @PostMapping
    @PreAuthorize("hasAnyRole('OFFICER', 'CLINICIAN', 'ADMIN')")
    public ResponseEntity<ApiResponse<InventoryResponse>> addStock(
            @Valid @RequestBody AddInventoryRequest request,
            Authentication auth) {
        InventoryResponse response = inventoryService.addStock(request, auth);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Stock added successfully", response));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('OFFICER', 'CLINICIAN', 'ADMIN')")
    public ResponseEntity<ApiResponse<InventoryResponse>> updateStock(
            @PathVariable Long id,
            @Valid @RequestBody UpdateInventoryRequest request,
            Authentication auth) {
        InventoryResponse response = inventoryService.updateStock(id, request, auth);
        return ResponseEntity.ok(ApiResponse.ok("Stock updated successfully", response));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('OFFICER', 'CLINICIAN', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<InventoryResponse>>> getInventory(Authentication auth) {
        List<InventoryResponse> inventory = inventoryService.getHospitalInventory(auth);
        return ResponseEntity.ok(ApiResponse.ok("Inventory retrieved", inventory));
    }

    @GetMapping("/{id}/audit")
    @PreAuthorize("hasAnyRole('OFFICER', 'CLINICIAN', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<AuditLogResponse>>> getAuditLog(
            @PathVariable Long id,
            Authentication auth) {
        List<AuditLogResponse> logs = inventoryService.getAuditLog(id, auth);
        return ResponseEntity.ok(ApiResponse.ok("Audit log retrieved", logs));
    }
}
