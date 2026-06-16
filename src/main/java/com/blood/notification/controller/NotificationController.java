package com.blood.notification.controller;

import com.blood.notification.dto.ApiResponse;
import com.blood.notification.dto.NotificationResponse;
import com.blood.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> listNotifications() {
        List<NotificationResponse> notifications = notificationService.listNotifications();
        return ResponseEntity.ok(ApiResponse.ok("Notifications retrieved", notifications));
    }
}
