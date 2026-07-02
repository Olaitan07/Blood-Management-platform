package com.blood.notification.service;

import com.blood.notification.dto.DeadLetterResponse;
import com.blood.notification.dto.NotificationResponse;

import java.util.List;

public interface NotificationService {

    List<NotificationResponse> listNotifications();

    List<NotificationResponse> listByHospital(Long hospitalId);

    List<NotificationResponse> listByDonor(Long donorId);

    List<DeadLetterResponse> listDeadLetters();
}
