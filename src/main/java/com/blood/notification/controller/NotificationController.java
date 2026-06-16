package com.blood.notification.controller;

import com.blood.notification.dto.ApiResponse;
import com.blood.notification.dto.DeadLetterResponse;
import com.blood.notification.dto.NotificationResponse;
import com.blood.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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

    /** Notifications for a specific hospital. */
    @GetMapping("/hospital/{hospitalId}")
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> listByHospital(
            @PathVariable Long hospitalId) {
        return ResponseEntity.ok(ApiResponse.ok("Notifications retrieved",
                notificationService.listByHospital(hospitalId)));
    }

    /** Notifications for a specific donor. */
    @GetMapping("/donor/{donorId}")
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> listByDonor(
            @PathVariable Long donorId) {
        return ResponseEntity.ok(ApiResponse.ok("Notifications retrieved",
                notificationService.listByDonor(donorId)));
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
}
