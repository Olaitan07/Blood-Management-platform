package com.blood.notification.controller;

import com.blood.notification.dto.ApiResponse;
import com.blood.notification.dto.DeadLetterResponse;
import com.blood.notification.dto.NotificationResponse;
import com.blood.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    /** All notifications — admin only. */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> listNotifications() {
        return ResponseEntity.ok(ApiResponse.ok("Notifications retrieved",
                notificationService.listNotifications()));
    }

    /** Notifications for a specific hospital — the caller's own, unless admin. */
    @GetMapping("/hospital/{hospitalId}")
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> listByHospital(
            @PathVariable Long hospitalId, Authentication auth) {
        return ResponseEntity.ok(ApiResponse.ok("Notifications retrieved",
                notificationService.listByHospital(hospitalId, auth)));
    }

    /** Notifications for a specific donor — the caller's own, unless admin. */
    @GetMapping("/donor/{donorId}")
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> listByDonor(
            @PathVariable Long donorId, Authentication auth) {
        return ResponseEntity.ok(ApiResponse.ok("Notifications retrieved",
                notificationService.listByDonor(donorId, auth)));
    }

    /**
     * Dead-letter queue — admin only.
     * Lists notifications that exhausted all retry attempts for manual review.
     */
    @GetMapping("/dead-letters")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<DeadLetterResponse>>> listDeadLetters() {
        return ResponseEntity.ok(ApiResponse.ok("Dead-letter notifications retrieved",
                notificationService.listDeadLetters()));
    }

    /** Marks a single notification read — the caller must own it, unless admin. */
    @PutMapping("/{id}/read")
    public ResponseEntity<ApiResponse<NotificationResponse>> markRead(
            @PathVariable Long id, Authentication auth) {
        return ResponseEntity.ok(ApiResponse.ok("Notification marked read",
                notificationService.markRead(id, auth)));
    }

    /** Marks every notification for a hospital read — the caller's own, unless admin. */
    @PutMapping("/hospital/{hospitalId}/read-all")
    public ResponseEntity<ApiResponse<Void>> markAllReadForHospital(
            @PathVariable Long hospitalId, Authentication auth) {
        notificationService.markAllReadForHospital(hospitalId, auth);
        return ResponseEntity.ok(ApiResponse.ok("Notifications marked read", null));
    }

    /** Marks every notification for a donor read — the caller's own, unless admin. */
    @PutMapping("/donor/{donorId}/read-all")
    public ResponseEntity<ApiResponse<Void>> markAllReadForDonor(
            @PathVariable Long donorId, Authentication auth) {
        notificationService.markAllReadForDonor(donorId, auth);
        return ResponseEntity.ok(ApiResponse.ok("Notifications marked read", null));
    }
}
