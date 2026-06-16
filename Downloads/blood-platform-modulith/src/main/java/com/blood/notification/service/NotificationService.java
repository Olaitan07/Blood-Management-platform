package com.blood.notification.service;

import com.blood.notification.dto.NotificationResponse;

import java.util.List;

public interface NotificationService {

    List<NotificationResponse> listNotifications();
}
