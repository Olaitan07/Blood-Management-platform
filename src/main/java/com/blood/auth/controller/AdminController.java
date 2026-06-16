package com.blood.auth.controller;

import com.blood.auth.dto.ApiResponse;
import com.blood.auth.dto.UserResponse;
import com.blood.auth.model.Role;
import com.blood.auth.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<List<UserResponse>>> listAllUsers() {
        List<UserResponse> users = adminService.listAllUsers();
        return ResponseEntity.ok(ApiResponse.ok("Users retrieved", users));
    }

    @GetMapping("/users/pending")
    public ResponseEntity<ApiResponse<List<UserResponse>>> listPendingUsers() {
        List<UserResponse> users = adminService.listPendingUsers();
        return ResponseEntity.ok(ApiResponse.ok("Pending users retrieved", users));
    }

    @PutMapping("/users/{id}/approve")
    public ResponseEntity<ApiResponse<UserResponse>> approveUser(@PathVariable Long id) {
        UserResponse user = adminService.approveUser(id);
        return ResponseEntity.ok(ApiResponse.ok("User approved successfully", user));
    }

    @PutMapping("/users/{id}/deactivate")
    public ResponseEntity<ApiResponse<UserResponse>> deactivateUser(@PathVariable Long id) {
        UserResponse user = adminService.deactivateUser(id);
        return ResponseEntity.ok(ApiResponse.ok("User deactivated successfully", user));
    }

    @PutMapping("/users/{id}/role")
    public ResponseEntity<ApiResponse<UserResponse>> updateUserRole(@PathVariable Long id,
                                                                     @RequestParam Role role) {
        UserResponse user = adminService.updateUserRole(id, role);
        return ResponseEntity.ok(ApiResponse.ok("User role updated successfully", user));
    }
}
